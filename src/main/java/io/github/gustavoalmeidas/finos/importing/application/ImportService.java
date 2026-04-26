package io.github.gustavoalmeidas.finos.importing.application;

import io.github.gustavoalmeidas.finos.identity.application.UserService;
import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.importing.domain.ImportBatch;
import io.github.gustavoalmeidas.finos.importing.domain.ImportBatchStatus;
import io.github.gustavoalmeidas.finos.importing.domain.ImportedRecord;
import io.github.gustavoalmeidas.finos.importing.domain.ImportedRecordStatus;
import io.github.gustavoalmeidas.finos.importing.dto.ImportBatchResponse;
import io.github.gustavoalmeidas.finos.importing.dto.ImportedRecordResponse;
import io.github.gustavoalmeidas.finos.importing.infrastructure.ImportBatchRepository;
import io.github.gustavoalmeidas.finos.importing.infrastructure.ImportedRecordRepository;
import io.github.gustavoalmeidas.finos.importing.mapper.ImportMapper;
import io.github.gustavoalmeidas.finos.ledger.application.AccountService;
import io.github.gustavoalmeidas.finos.ledger.application.CategoryService;
import io.github.gustavoalmeidas.finos.ledger.application.TransactionService;
import io.github.gustavoalmeidas.finos.ledger.domain.Account;
import io.github.gustavoalmeidas.finos.ledger.domain.Category;
import io.github.gustavoalmeidas.finos.ledger.domain.CategoryType;
import io.github.gustavoalmeidas.finos.ledger.domain.Transaction;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionSource;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionType;
import io.github.gustavoalmeidas.finos.ledger.dto.CreateTransactionRequest;
import io.github.gustavoalmeidas.finos.shared.exception.BusinessException;
import io.github.gustavoalmeidas.finos.shared.exception.NotFoundException;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImportService {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );

    private final UserService userService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final TransactionService transactionService;
    private final ImportBatchRepository batchRepository;
    private final ImportedRecordRepository recordRepository;
    private final ImportMapper mapper;

    @Transactional
    public ImportBatchResponse createBatchFromCsv(Long accountId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("import.file.empty", "Selecione um arquivo CSV para importar.");
        }

        User user = userService.currentUser();
        Account account = accountService.getOwnedEntity(accountId);

        ImportBatch batch = new ImportBatch();
        batch.setUser(user);
        batch.setAccount(account);
        batch.setFilename(file.getOriginalFilename() == null ? "importacao.csv" : file.getOriginalFilename());
        batch.setFileSize(file.getSize());
        batch.setStatus(ImportBatchStatus.RECEIVED);
        batch = batchRepository.save(batch);

        Set<String> hashesInBatch = new HashSet<>();
        int total = 0;
        int valid = 0;
        int invalid = 0;
        int duplicates = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build()) {
            List<String[]> rows = csvReader.readAll();
            int lineNumber = 2;
            for (String[] row : rows) {
                total++;
                ImportedRecord record = parseRecord(batch, account, row, lineNumber);
                if (record.getStatus() == ImportedRecordStatus.VALID && !hashesInBatch.add(record.getDeduplicationHash())) {
                    record.setStatus(ImportedRecordStatus.DUPLICATE);
                    record.setErrorMessage("Registro duplicado dentro do mesmo arquivo.");
                }
                if (record.getStatus() == ImportedRecordStatus.VALID
                        && recordRepository.existsByDeduplicationHashAndStatus(record.getDeduplicationHash(), ImportedRecordStatus.IMPORTED)) {
                    record.setStatus(ImportedRecordStatus.DUPLICATE);
                    record.setErrorMessage("Registro já importado anteriormente.");
                }
                if (record.getStatus() == ImportedRecordStatus.VALID) valid++;
                if (record.getStatus() == ImportedRecordStatus.INVALID) invalid++;
                if (record.getStatus() == ImportedRecordStatus.DUPLICATE) duplicates++;
                recordRepository.save(record);
                lineNumber++;
            }
            batch.setStatus(ImportBatchStatus.PREVIEWED);
        } catch (Exception ex) {
            batch.setStatus(ImportBatchStatus.FAILED);
            batch.setErrorMessage(ex.getMessage());
        }

        batch.setTotalRecords(total);
        batch.setValidRecords(valid);
        batch.setInvalidRecords(invalid);
        batch.setDuplicateRecords(duplicates);
        return mapper.toBatchResponse(batchRepository.save(batch));
    }

    @Transactional(readOnly = true)
    public Page<ImportBatchResponse> list(ImportBatchStatus status, Pageable pageable) {
        User user = userService.currentUser();
        Page<ImportBatch> page = status == null
                ? batchRepository.findByUser(user, pageable)
                : batchRepository.findByUserAndStatus(user, status, pageable);
        return page.map(mapper::toBatchResponse);
    }

    @Transactional(readOnly = true)
    public ImportBatchResponse getBatch(Long batchId) {
        return mapper.toBatchResponse(getOwnedBatch(batchId));
    }

    @Transactional(readOnly = true)
    public Page<ImportedRecordResponse> previewBatch(Long batchId, Pageable pageable) {
        ImportBatch batch = getOwnedBatch(batchId);
        return recordRepository.findByBatch(batch, pageable).map(mapper::toRecordResponse);
    }

    @Transactional
    public ImportBatchResponse confirmImport(Long batchId) {
        ImportBatch batch = getOwnedBatch(batchId);
        if (batch.getStatus() == ImportBatchStatus.CANCELLED || batch.getStatus() == ImportBatchStatus.FAILED) {
            throw new BusinessException("import.batch.not-confirmable", "Este lote não pode ser confirmado.");
        }

        batch.setStatus(ImportBatchStatus.PROCESSING);
        List<ImportedRecord> validRecords = recordRepository.findByBatchAndStatus(batch, ImportedRecordStatus.VALID);
        int imported = 0;
        for (ImportedRecord record : validRecords) {
            Category category = categoryService.findOrCreateImportCategory(
                    batch.getUser(),
                    record.getParsedType() == TransactionType.INCOME ? CategoryType.INCOME : CategoryType.EXPENSE
            );
            Transaction transaction = transactionService.createEntity(new CreateTransactionRequest(
                    batch.getAccount().getId(),
                    null,
                    record.getParsedType(),
                    null,
                    TransactionSource.CSV_IMPORT,
                    record.getParsedAmount(),
                    record.getParsedDate(),
                    record.getParsedDescription(),
                    "Importado do lote " + batch.getId(),
                    category.getId(),
                    null,
                    Set.of(),
                    List.of(),
                    batch.getId(),
                    null,
                    null,
                    null
            ));
            record.setTransaction(transaction);
            record.setStatus(ImportedRecordStatus.IMPORTED);
            recordRepository.save(record);
            imported++;
        }
        batch.setProcessedAt(LocalDateTime.now());
        batch.setStatus(imported == batch.getValidRecords() ? ImportBatchStatus.IMPORTED : ImportBatchStatus.PARTIALLY_IMPORTED);
        return mapper.toBatchResponse(batchRepository.save(batch));
    }

    @Transactional
    public ImportBatchResponse cancelBatch(Long batchId) {
        ImportBatch batch = getOwnedBatch(batchId);
        batch.setStatus(ImportBatchStatus.CANCELLED);
        batch.setProcessedAt(LocalDateTime.now());
        return mapper.toBatchResponse(batchRepository.save(batch));
    }

    private ImportBatch getOwnedBatch(Long batchId) {
        User user = userService.currentUser();
        return batchRepository.findByIdAndUser(batchId, user)
                .orElseThrow(() -> new NotFoundException("import.batch.not-found", "Lote de importação não encontrado."));
    }

    private ImportedRecord parseRecord(ImportBatch batch, Account account, String[] row, int lineNumber) {
        ImportedRecord record = new ImportedRecord();
        record.setBatch(batch);
        record.setLineNumber(lineNumber);
        record.setRawPayload(String.join(",", row));
        try {
            if (row.length < 4) {
                throw new IllegalArgumentException("Formato esperado: data, valor, tipo, descrição.");
            }
            LocalDate date = parseDate(row[0].trim());
            BigDecimal amount = parseAmount(row[1].trim()).abs();
            TransactionType type = parseType(row[2].trim(), parseAmount(row[1].trim()));
            String description = row[3].trim();
            record.setParsedDate(date);
            record.setParsedAmount(amount);
            record.setParsedType(type);
            record.setParsedDescription(description);
            record.setDeduplicationHash(hash(account.getId() + "|" + date + "|" + amount + "|" + description.toLowerCase()));
            record.setStatus(ImportedRecordStatus.VALID);
        } catch (Exception ex) {
            record.setDeduplicationHash(hash(batch.getId() + "|" + lineNumber + "|" + Arrays.toString(row)));
            record.setStatus(ImportedRecordStatus.INVALID);
            record.setErrorMessage(ex.getMessage());
        }
        return record;
    }

    private LocalDate parseDate(String value) {
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (Exception ignored) {
            }
        }
        throw new IllegalArgumentException("Data inválida. Use yyyy-MM-dd ou dd/MM/yyyy.");
    }

    private BigDecimal parseAmount(String value) {
        String normalized = value.replace(".", "").replace(",", ".");
        return new BigDecimal(normalized);
    }

    private TransactionType parseType(String value, BigDecimal signedAmount) {
        if (value == null || value.isBlank()) {
            return signedAmount.signum() < 0 ? TransactionType.EXPENSE : TransactionType.INCOME;
        }
        return TransactionType.valueOf(value.toUpperCase());
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível calcular hash de deduplicação.", ex);
        }
    }
}
