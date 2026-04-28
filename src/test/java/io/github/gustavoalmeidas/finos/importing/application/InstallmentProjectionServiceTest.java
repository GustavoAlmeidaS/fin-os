package io.github.gustavoalmeidas.finos.importing.application;

import io.github.gustavoalmeidas.finos.ledger.domain.Transaction;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionStatus;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstallmentProjectionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private IdempotencyKeyService idempotencyKeyService;

    @InjectMocks
    private InstallmentProjectionService projectionService;

    @Captor
    private ArgumentCaptor<Transaction> txCaptor;

    private Transaction originalTx;

    @BeforeEach
    void setUp() {
        originalTx = new Transaction();
        originalTx.setId(99L);
        originalTx.setStatus(TransactionStatus.POSTED);
        originalTx.setTransactionDate(LocalDate.of(2026, 12, 15)); // December boundary
        originalTx.setIdempotencyKey("ORIGINAL_KEY");
        originalTx.setInstallmentInfo("1/3");
        originalTx.setRawDescription("COMPRA LOJA 1/3");
        originalTx.setDescription("COMPRA LOJA 1/3");
    }

    @Test
    void shouldProjectFutureInstallmentsCorrectly() {
        when(idempotencyKeyService.generateProjectedKey("ORIGINAL_KEY", 2)).thenReturn("KEY2");
        when(idempotencyKeyService.generateProjectedKey("ORIGINAL_KEY", 3)).thenReturn("KEY3");
        when(transactionRepository.existsByIdempotencyKey(any())).thenReturn(false);

        projectionService.projectFutureInstallments(originalTx);

        verify(transactionRepository, times(2)).save(txCaptor.capture());
        List<Transaction> savedTxs = txCaptor.getAllValues();

        Transaction tx2 = savedTxs.get(0);
        assertEquals(TransactionStatus.PENDING, tx2.getStatus());
        assertEquals(LocalDate.of(2027, 1, 15), tx2.getTransactionDate()); // Advanced to next year correctly
        assertEquals("2/3", tx2.getInstallmentInfo());
        assertEquals("COMPRA LOJA 2/3", tx2.getRawDescription());
        assertEquals("KEY2", tx2.getIdempotencyKey());

        Transaction tx3 = savedTxs.get(1);
        assertEquals(TransactionStatus.PENDING, tx3.getStatus());
        assertEquals(LocalDate.of(2027, 2, 15), tx3.getTransactionDate());
        assertEquals("3/3", tx3.getInstallmentInfo());
        assertEquals("COMPRA LOJA 3/3", tx3.getRawDescription());
        assertEquals("KEY3", tx3.getIdempotencyKey());
    }

    @Test
    void shouldNotProjectIfAlreadyFinalInstallment() {
        originalTx.setInstallmentInfo("3/3");
        originalTx.setRawDescription("COMPRA LOJA 3/3");

        projectionService.projectFutureInstallments(originalTx);

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void shouldNotProjectIfStatusNotPosted() {
        originalTx.setStatus(TransactionStatus.PENDING);
        
        projectionService.projectFutureInstallments(originalTx);

        verify(transactionRepository, never()).save(any());
    }
}
