package io.github.gustavoalmeidas.finos.planning.dto;

import java.math.BigDecimal;

public record CategoryBudgetResponse(
        Long id,
        Long categoryId,
        String categoryName,
        BigDecimal amountLimit,
        boolean active
) {
}
