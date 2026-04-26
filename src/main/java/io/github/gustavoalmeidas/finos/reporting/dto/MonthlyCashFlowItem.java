package io.github.gustavoalmeidas.finos.reporting.dto;

import java.math.BigDecimal;

public record MonthlyCashFlowItem(
        String yearMonth,
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal net
) {
}
