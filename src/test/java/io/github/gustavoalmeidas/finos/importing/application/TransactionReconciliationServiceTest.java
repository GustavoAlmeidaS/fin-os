package io.github.gustavoalmeidas.finos.importing.application;

import io.github.gustavoalmeidas.finos.importing.domain.ImportedTransactionRaw;
import io.github.gustavoalmeidas.finos.ledger.domain.Account;
import io.github.gustavoalmeidas.finos.ledger.domain.Transaction;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionReconciliationServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionReconciliationService reconciliationService;

    @Test
    void shouldFindClosestMatchWithinDateWindow() {
        Account account = new Account();
        account.setId(10L);

        ImportedTransactionRaw raw = ImportedTransactionRaw.builder()
                .transactionDate(LocalDate.of(2026, 5, 14)) // CSV Date
                .normalizedAmount(new BigDecimal("-50.00"))
                .installmentInfo("2/3")
                .build();

        // Let's create two candidates inside the DB. Both are projected, but dates vary.
        Transaction projected1 = new Transaction();
        projected1.setTransactionDate(LocalDate.of(2026, 5, 10)); // 4 days apart

        Transaction projected2 = new Transaction();
        projected2.setTransactionDate(LocalDate.of(2026, 5, 15)); // 1 day apart, closer!

        when(transactionRepository.findProjectedMatches(
                10L,
                new BigDecimal("-50.00"),
                "2/3",
                LocalDate.of(2026, 5, 9),
                LocalDate.of(2026, 5, 19)
        )).thenReturn(List.of(projected1, projected2));

        Optional<Transaction> match = reconciliationService.findMatchFor(raw, account);

        assertTrue(match.isPresent());
        assertEquals(projected2, match.get(), "Should select the transaction with the closest date");
    }

    @Test
    void shouldReturnEmptyIfNoMatches() {
        Account account = new Account();
        account.setId(10L);

        ImportedTransactionRaw raw = ImportedTransactionRaw.builder()
                .transactionDate(LocalDate.of(2026, 5, 14))
                .normalizedAmount(new BigDecimal("-50.00"))
                .installmentInfo("2/3")
                .build();

        when(transactionRepository.findProjectedMatches(
                10L,
                new BigDecimal("-50.00"),
                "2/3",
                LocalDate.of(2026, 5, 9),
                LocalDate.of(2026, 5, 19)
        )).thenReturn(List.of());

        Optional<Transaction> match = reconciliationService.findMatchFor(raw, account);

        assertTrue(match.isEmpty());
    }
}
