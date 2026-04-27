package io.github.gustavoalmeidas.finos.importing.application;

import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.importing.domain.ImportContext;
import io.github.gustavoalmeidas.finos.importing.domain.ImportedTransactionRaw;
import io.github.gustavoalmeidas.finos.importing.domain.StatementDetectionContext;
import io.github.gustavoalmeidas.finos.ledger.domain.Account;
import io.github.gustavoalmeidas.finos.ledger.domain.Category;
import io.github.gustavoalmeidas.finos.importing.domain.ImportBatch;
import io.github.gustavoalmeidas.finos.importing.domain.ImportBatchStatus;
import io.github.gustavoalmeidas.finos.importing.infrastructure.ImportBatchRepository;
import io.github.gustavoalmeidas.finos.ledger.domain.Transaction;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionSource;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionStatus;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionType;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionImportService {

    private final StatementParserRegistry parserRegistry;
    private final IdempotencyKeyService idempotencyKeyService;
    private final DescriptionSanitizer sanitizer;
    private final TransactionClassifier classifier;
    private final ImportBatchRepository batchRepository;
    private final TransactionRepository transactionRepository;

    public TransactionImportService(StatementParserRegistry parserRegistry,
                                    IdempotencyKeyService idempotencyKeyService,
                                    DescriptionSanitizer sanitizer,
                                    TransactionClassifier classifier,
                                    ImportBatchRepository batchRepository,
                                    TransactionRepository transactionRepository) {
        this.parserRegistry = parserRegistry;
        this.idempotencyKeyService = idempotencyKeyService;
        this.sanitizer = sanitizer;
        this.classifier = classifier;
        this.batchRepository = batchRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public ImportBatch processImport(InputStream inputStream, String fileName, Account account, User user) throws Exception {
        byte[] fileBytes = inputStream.readAllBytes();
        String firstLine = extractFirstLine(fileBytes);

        StatementDetectionContext detectionContext = StatementDetectionContext.builder()
                .fileName(fileName)
                .firstLine(firstLine)
                .fileContent(fileBytes)
                .build();

        StatementParser parser = parserRegistry.findParser(detectionContext)
                .orElseThrow(() -> new IllegalArgumentException("No suitable parser found for file: " + fileName));

        ImportContext importContext = ImportContext.builder()
                .account(account)
                .fileName(fileName)
                .build();

        ImportBatch batch = new ImportBatch();
        batch.setFilename(fileName);
        batch.setProcessedAt(LocalDateTime.now());
        batch.setAccount(account);
        batch.setUser(user);
        batch.setStatus(ImportBatchStatus.PROCESSING);
        batch = batchRepository.save(batch);

        List<ImportedTransactionRaw> rawTransactions;
        try {
            rawTransactions = parser.parse(new ByteArrayInputStream(fileBytes), importContext);
        } catch (Exception e) {
            batch.setStatus(ImportBatchStatus.FAILED);
            batch.setErrorMessage(e.getMessage());
            batchRepository.save(batch);
            throw e;
        }

        int imported = 0;
        int duplicates = 0;

        for (ImportedTransactionRaw raw : rawTransactions) {
            String sanitizedDesc = sanitizer.sanitize(raw.getRawDescription());
            
            String idempotencyKey = idempotencyKeyService.generateKey(
                    account.getId(),
                    raw.getTransactionDate(),
                    raw.getNormalizedAmount(),
                    sanitizedDesc,
                    raw.getInstallmentInfo(),
                    raw.getOccurrenceIndex(),
                    raw.getExternalId()
            );

            // Deduplication check
            if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
                duplicates++;
                continue;
            }

            Transaction tx = new Transaction();
            tx.setIdempotencyKey(idempotencyKey);
            tx.setExternalId(raw.getExternalId());
            tx.setAccount(account);
            tx.setUser(user);
            tx.setImportBatch(batch);
            
            // Assume imported rows are always posted (can be adjusted by rules later)
            tx.setStatus(TransactionStatus.POSTED);
            tx.setSource(TransactionSource.CSV_IMPORT);
            
            tx.setTransactionDate(raw.getTransactionDate());
            tx.setAmount(raw.getNormalizedAmount());
            tx.setRawDescription(raw.getRawDescription());
            tx.setSearchableDescription(sanitizedDesc);
            tx.setDescription(raw.getRawDescription().length() > 255 ? raw.getRawDescription().substring(0, 255) : raw.getRawDescription());
            tx.setInstallmentInfo(raw.getInstallmentInfo());
            tx.setRawRowPayload(raw.getRawRowPayload());
            
            tx.setType(raw.getNormalizedAmount().signum() >= 0 ? TransactionType.INCOME : TransactionType.EXPENSE);

            // Classification
            Optional<Category> category = classifier.classify(sanitizedDesc, user.getId());
            category.ifPresent(tx::setCategory);

            transactionRepository.save(tx);
            imported++;
        }

        batch.setTotalRecords(rawTransactions.size());
        batch.setValidRecords(imported);
        batch.setDuplicateRecords(duplicates);
        batch.setStatus(ImportBatchStatus.IMPORTED);

        return batchRepository.save(batch);
    }

    private String extractFirstLine(byte[] fileBytes) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fileBytes), StandardCharsets.UTF_8))) {
            return reader.readLine();
        } catch (Exception e) {
            return null;
        }
    }
}
