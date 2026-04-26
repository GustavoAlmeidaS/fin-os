package io.github.gustavoalmeidas.finos.reporting.dto;

import java.util.List;

public record CashFlowMonthlyResponse(
        List<MonthlyCashFlowItem> months
) {
}
