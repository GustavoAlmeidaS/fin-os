package io.github.gustavoalmeidas.finos.reporting.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal netCashFlow,
        BigDecimal currentMonthIncome,
        BigDecimal currentMonthExpenses,
        BigDecimal currentMonthNetCashFlow,
        long accountCount,
        long transactionCount,
        BigDecimal totalAssets,
        BigDecimal totalDebt,
        BigDecimal netWorth
) {
}
