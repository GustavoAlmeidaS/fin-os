package io.github.gustavoalmeidas.finos.importing.application;

import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.importing.domain.ImportBatch;
import io.github.gustavoalmeidas.finos.importing.domain.ImportedTransactionRaw;
import io.github.gustavoalmeidas.finos.importing.domain.StatementDetectionContext;
import io.github.gustavoalmeidas.finos.importing.infrastructure.ImportBatchRepository;
import io.github.gustavoalmeidas.finos.ledger.domain.Account;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.AccountRepository;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionImportServiceTest {

    @Mock private StatementParserRegistry parserRegistry;
    @Mock private IdempotencyKeyService idempotencyKeyService;
    @Mock private DescriptionSanitizer sanitizer;
    @Mock private TransactionClassifier classifier;
    @Mock private ImportBatchRepository batchRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private InstallmentProjectionService projectionService;
    @Mock private TransactionReconciliationService reconciliationService;
    @Mock private AccountRepository accountRepository;
    @Mock private StatementParser parser;

    @InjectMocks
    private TransactionImportService importService;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    private Account account;
    private User user;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(10L);
        account.setCurrentBalance(new BigDecimal("100.00")); // Initial Balance
        
        user = new User();
        user.setId(1L);
    }

    @Test
    void shouldUpdateAccountBalanceCorrectlyOnImport() throws Exception {
        // Arrange
        when(parserRegistry.findParser(any(StatementDetectionContext.class))).thenReturn(Optional.of(parser));
        when(batchRepository.save(any(ImportBatch.class))).thenAnswer(i -> i.getArgument(0));
        
        ImportedTransactionRaw expense = ImportedTransactionRaw.builder()
                .transactionDate(LocalDate.now())
                .normalizedAmount(new BigDecimal("-67.90")) // Expense
                .rawDescription("DEBITO VISA")
                .sourceType("SANTANDER")
                .build();

        ImportedTransactionRaw income = ImportedTransactionRaw.builder()
                .transactionDate(LocalDate.now())
                .normalizedAmount(new BigDecimal("70.00")) // Income
                .rawDescription("PIX RECEBIDO")
                .sourceType("SANTANDER")
                .build();

        when(parser.parse(any(), any())).thenReturn(List.of(expense, income));
        when(sanitizer.sanitize(anyString())).thenReturn("Clean Desc");
        when(idempotencyKeyService.generateKey(any(), any(), any(), any(), any(), anyInt(), any())).thenReturn("hash");
        when(transactionRepository.existsByIdempotencyKey(any())).thenReturn(false);
        when(reconciliationService.findMatchFor(any(), any())).thenReturn(Optional.empty());

        // Act
        importService.processImport(new ByteArrayInputStream("dummy".getBytes()), "file.csv", account, user);

        // Assert
        // Starting balance: 100.00
        // Expense: -67.90
        // Income: +70.00
        // Expected ending balance: 102.10
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        
        assertEquals(new BigDecimal("102.10"), savedAccount.getCurrentBalance());
    }
}
