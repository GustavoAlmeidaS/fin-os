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
import io.github.gustavoalmeidas.finos.ledger.infrastructure.AccountRepository;
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
import java.math.BigDecimal;

@Service
public class TransactionImportService {

    private final StatementParserRegistry parserRegistry;
    private final IdempotencyKeyService idempotencyKeyService;
    private final DescriptionSanitizer sanitizer;
    private final TransactionClassifier classifier;
    private final ImportBatchRepository batchRepository;
    private final TransactionRepository transactionRepository;
    private final InstallmentProjectionService projectionService;
    private final TransactionReconciliationService reconciliationService;
    private final AccountRepository accountRepository;

    public TransactionImportService(StatementParserRegistry parserRegistry,
                                    IdempotencyKeyService idempotencyKeyService,
                                    DescriptionSanitizer sanitizer,
                                    TransactionClassifier classifier,
                                    ImportBatchRepository batchRepository,
                                    TransactionRepository transactionRepository,
                                    InstallmentProjectionService projectionService,
                                    TransactionReconciliationService reconciliationService,
                                    AccountRepository accountRepository) {
        this.parserRegistry = parserRegistry;
        this.idempotencyKeyService = idempotencyKeyService;
        this.sanitizer = sanitizer;
        this.classifier = classifier;
        this.batchRepository = batchRepository;
        this.transactionRepository = transactionRepository;
        this.projectionService = projectionService;
        this.reconciliationService = reconciliationService;
        this.accountRepository = accountRepository;
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
        BigDecimal balanceDelta = BigDecimal.ZERO;

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

            Optional<Transaction> reconciledMatch = reconciliationService.findMatchFor(raw, account);
            Transaction tx;

            if (reconciledMatch.isPresent()) {
                tx = reconciledMatch.get();
                // Match Found: UPDATE projected transaction to CLEARED/POSTED
                tx.setStatus(TransactionStatus.POSTED);
                tx.setTransactionDate(raw.getTransactionDate());
                tx.setIdempotencyKey(idempotencyKey);
                tx.setExternalId(raw.getExternalId());
                tx.setRawDescription(raw.getRawDescription());
                tx.setSearchableDescription(sanitizedDesc);
                tx.setDescription(raw.getRawDescription().length() > 255 ? raw.getRawDescription().substring(0, 255) : raw.getRawDescription());
                tx.setInstallmentInfo(raw.getInstallmentInfo());
                tx.setRawRowPayload(raw.getRawRowPayload());
                // We leave category/counterparty untouched so any manual user edits on projected tx are preserved
            } else {
                // No Match Found: INSERT new transaction
                tx = new Transaction();
                tx.setIdempotencyKey(idempotencyKey);
                tx.setExternalId(raw.getExternalId());
                tx.setAccount(account);
                tx.setUser(user);
                tx.setImportBatch(batch);
                
                // Assume imported rows are always posted (can be adjusted by rules later)
                tx.setStatus(TransactionStatus.POSTED);
                tx.setSource(TransactionSource.CSV_IMPORT);
                
                tx.setTransactionDate(raw.getTransactionDate());
                tx.setAmount(raw.getNormalizedAmount().abs());
                tx.setRawDescription(raw.getRawDescription());
                tx.setSearchableDescription(sanitizedDesc);
                tx.setDescription(raw.getRawDescription().length() > 255 ? raw.getRawDescription().substring(0, 255) : raw.getRawDescription());
                tx.setInstallmentInfo(raw.getInstallmentInfo());
                tx.setRawRowPayload(raw.getRawRowPayload());
                
                tx.setType(raw.getNormalizedAmount().signum() >= 0 ? TransactionType.INCOME : TransactionType.EXPENSE);

                // Classification
                Optional<Category> category = classifier.classify(sanitizedDesc, user.getId());
                if (category.isPresent()) {
                    tx.setCategory(category.get());
                    // Sync TransactionType with CategoryType to fix "Pagamento recebido" as INCOME
                    if (category.get().getType() == io.github.gustavoalmeidas.finos.ledger.domain.CategoryType.TRANSFER || 
                        category.get().getType() == io.github.gustavoalmeidas.finos.ledger.domain.CategoryType.ADJUSTMENT) {
                        tx.setType(TransactionType.valueOf(category.get().getType().name()));
                    }
                }
            }

            transactionRepository.save(tx);
            projectionService.projectFutureInstallments(tx);
            imported++;

            if (tx.getStatus() == TransactionStatus.POSTED) {
                if (tx.getType() == TransactionType.INCOME || tx.getType() == TransactionType.ADJUSTMENT) {
                    balanceDelta = balanceDelta.add(tx.getAmount());
                } else if (tx.getType() == TransactionType.EXPENSE) {
                    balanceDelta = balanceDelta.subtract(tx.getAmount());
                }
            }
        }

        if (balanceDelta.compareTo(BigDecimal.ZERO) != 0) {
            account.setCurrentBalance(account.getCurrentBalance().add(balanceDelta));
            accountRepository.save(account);
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
