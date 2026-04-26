package io.github.gustavoalmeidas.finos.reporting.api;

import io.github.gustavoalmeidas.finos.ledger.domain.TransactionSource;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionStatus;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionType;
import io.github.gustavoalmeidas.finos.reporting.application.ReportingService;
import io.github.gustavoalmeidas.finos.reporting.dto.BiTransactionExportResponse;
import io.github.gustavoalmeidas.finos.reporting.dto.CashFlowMonthlyResponse;
import io.github.gustavoalmeidas.finos.reporting.dto.CategorySummaryResponse;
import io.github.gustavoalmeidas.finos.reporting.dto.DashboardSummaryResponse;
import io.github.gustavoalmeidas.finos.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/dashboard-summary")
    public ApiResponse<DashboardSummaryResponse> dashboardSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.ok(reportingService.getDashboardSummary(startDate, endDate));
    }

    @GetMapping("/cash-flow/monthly")
    public ApiResponse<CashFlowMonthlyResponse> cashFlowMonthly(
            @RequestParam(required = false) String startYearMonth,
            @RequestParam(required = false) String endYearMonth
    ) {
        YearMonth start = startYearMonth == null || startYearMonth.isBlank() ? null : YearMonth.parse(startYearMonth);
        YearMonth end = endYearMonth == null || endYearMonth.isBlank() ? null : YearMonth.parse(endYearMonth);
        return ApiResponse.ok(reportingService.getMonthlyCashFlow(start, end));
    }

    @GetMapping("/categories/summary")
    public ApiResponse<CategorySummaryResponse> categorySummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) TransactionType type
    ) {
        return ApiResponse.ok(reportingService.getCategorySummary(startDate, endDate, type));
    }

    @GetMapping("/fact-transactions")
    public ApiResponse<Object> factTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionSource source
    ) {
        return ApiResponse.ok(reportingService.getFactTransactions(startDate, endDate, accountId, categoryId, type, source));
    }
}
