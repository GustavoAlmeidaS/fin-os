package io.github.gustavoalmeidas.finos.reporting.dto;

import io.github.gustavoalmeidas.finos.ledger.domain.TransactionSource;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionStatus;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BiTransactionItem(
        Long id,
        LocalDate date,
        TransactionType type,
        BigDecimal amount,
        String accountName,
        String categoryName,
        String counterpartyName,
        TransactionSource source,
        TransactionStatus status
) {
}
