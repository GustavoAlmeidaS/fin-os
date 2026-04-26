package io.github.gustavoalmeidas.finos.ledger.dto;

import java.math.BigDecimal;

public record TransactionSplitResponse(
        Long id,
        Long categoryId,
        String categoryName,
        BigDecimal amount,
        String description
) {
}
