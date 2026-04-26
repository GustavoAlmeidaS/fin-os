package io.github.gustavoalmeidas.finos.reporting.dto;

import io.github.gustavoalmeidas.finos.ledger.domain.TransactionType;

import java.math.BigDecimal;

public record CategorySummaryItem(
        Long categoryId,
        String categoryName,
        TransactionType type,
        BigDecimal totalAmount
) {
}
