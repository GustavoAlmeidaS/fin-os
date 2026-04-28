package io.github.gustavoalmeidas.finos.importing.application;

import io.github.gustavoalmeidas.finos.importing.domain.ImportedTransactionRaw;
import io.github.gustavoalmeidas.finos.ledger.domain.Account;
import io.github.gustavoalmeidas.finos.ledger.domain.Transaction;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionReconciliationService {

    private final TransactionRepository transactionRepository;

    public TransactionReconciliationService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Attempts to find a previously projected (PENDING) transaction that matches
     * the incoming real imported transaction.
     * Match heuristics:
     * - Same Account
     * - Status = PENDING
     * - Exact Amount
     * - Exact Installment Info
     * - Date within [-5, +5] days
     */
    public Optional<Transaction> findMatchFor(ImportedTransactionRaw raw, Account account) {
        String installmentInfo = raw.getInstallmentInfo();
        if (installmentInfo == null || installmentInfo.isBlank()) {
            return Optional.empty(); // We only reconcile installments according to the requirement
        }

        LocalDate targetDate = raw.getTransactionDate();
        LocalDate startDate = targetDate.minusDays(5);
        LocalDate endDate = targetDate.plusDays(5);

        List<Transaction> candidates = transactionRepository.findProjectedMatches(
                account.getId(),
                raw.getNormalizedAmount(),
                installmentInfo,
                startDate,
                endDate
        );

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        // Return the closest by date, or just the first if dates are same.
        return candidates.stream()
                .min((t1, t2) -> {
                    long diff1 = Math.abs(t1.getTransactionDate().toEpochDay() - targetDate.toEpochDay());
                    long diff2 = Math.abs(t2.getTransactionDate().toEpochDay() - targetDate.toEpochDay());
                    return Long.compare(diff1, diff2);
                });
    }
}
