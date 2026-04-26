package io.github.gustavoalmeidas.finos.planning.dto;

import java.math.BigDecimal;

public record CreateCategoryBudgetRequest(
        Long categoryId,
        BigDecimal amountLimit
) {
}
