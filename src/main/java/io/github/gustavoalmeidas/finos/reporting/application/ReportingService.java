package io.github.gustavoalmeidas.finos.reporting.application;

import io.github.gustavoalmeidas.finos.identity.application.UserService;
import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.ledger.domain.Transaction;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionSource;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionStatus;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionType;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.AccountRepository;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.TransactionRepository;
import io.github.gustavoalmeidas.finos.loan.domain.Loan;
import io.github.gustavoalmeidas.finos.loan.domain.LoanStatus;
import io.github.gustavoalmeidas.finos.loan.infrastructure.LoanRepository;
import io.github.gustavoalmeidas.finos.reporting.dto.BiTransactionExportResponse;
import io.github.gustavoalmeidas.finos.reporting.dto.BiTransactionItem;
import io.github.gustavoalmeidas.finos.reporting.dto.CashFlowMonthlyResponse;
import io.github.gustavoalmeidas.finos.reporting.dto.CategorySummaryItem;
import io.github.gustavoalmeidas.finos.reporting.dto.CategorySummaryResponse;
import io.github.gustavoalmeidas.finos.reporting.dto.DashboardSummaryResponse;
import io.github.gustavoalmeidas.finos.reporting.dto.MonthlyCashFlowItem;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final UserService userService;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final LoanRepository loanRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(LocalDate startDate, LocalDate endDate) {
        User user = userService.currentUser();
        List<Transaction> transactions = transactionRepository.findAll(byFilters(user, startDate, endDate, null, null, null, null, null));
        YearMonth currentMonth = YearMonth.now();
        Totals total = totals(transactions);
        Totals month = totals(transactions.stream()
                .filter(tx -> YearMonth.from(tx.getTransactionDate()).equals(currentMonth))
                .toList());
        
        BigDecimal totalAssets = accountRepository.findByUserOrderByNameAsc(user).stream()
                .filter(account -> account.isActive())
                .map(account -> account.getCurrentBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal totalDebt = loanRepository.findByUserOrderByNameAsc(user).stream()
                .filter(loan -> loan.getStatus() == LoanStatus.ACTIVE)
                .map(loan -> loan.getCurrentBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal netWorth = totalAssets.subtract(totalDebt);
        
        return new DashboardSummaryResponse(
                total.income,
                total.expenses,
                total.net(),
                month.income,
                month.expenses,
                month.net(),
                accountRepository.findByUserOrderByNameAsc(user).stream().filter(account -> account.isActive()).count(),
                transactions.size(),
                totalAssets,
                totalDebt,
                netWorth
        );
    }

    @Transactional(readOnly = true)
    public CashFlowMonthlyResponse getMonthlyCashFlow(YearMonth start, YearMonth end) {
        User user = userService.currentUser();
        YearMonth effectiveStart = start == null ? YearMonth.now().minusMonths(5) : start;
        YearMonth effectiveEnd = end == null ? YearMonth.now() : end;
        
        String sql = """
            SELECT year_month, total_income, total_expense, net_cashflow
            FROM v_monthly_cashflow
            WHERE user_id = ?
              AND year_month >= ?
              AND year_month <= ?
            ORDER BY year_month ASC
            """;
            
        List<MonthlyCashFlowItem> items = jdbcTemplate.query(sql, (rs, rowNum) -> new MonthlyCashFlowItem(
            rs.getString("year_month"),
            rs.getBigDecimal("total_income"),
            rs.getBigDecimal("total_expense"),
            rs.getBigDecimal("net_cashflow")
        ), user.getId(), effectiveStart.toString(), effectiveEnd.toString());
        
        return new CashFlowMonthlyResponse(items);
    }

    @Transactional(readOnly = true)
    public CategorySummaryResponse getCategorySummary(LocalDate startDate, LocalDate endDate, TransactionType type) {
        User user = userService.currentUser();
        List<Transaction> transactions = transactionRepository.findAll(byFilters(user, startDate, endDate, null, null, type, null, null));
        Map<String, CategorySummaryItem> grouped = new LinkedHashMap<>();
        for (Transaction tx : transactions) {
            if (tx.getType() == TransactionType.TRANSFER) {
                continue;
            }
            Long categoryId = tx.getCategory() == null ? null : tx.getCategory().getId();
            String categoryName = tx.getCategory() == null ? "Sem categoria" : tx.getCategory().getName();
            String key = categoryId + "|" + tx.getType();
            CategorySummaryItem previous = grouped.get(key);
            BigDecimal total = (previous == null ? BigDecimal.ZERO : previous.totalAmount()).add(tx.getAmount());
            grouped.put(key, new CategorySummaryItem(categoryId, categoryName, tx.getType(), total));
        }
        return new CategorySummaryResponse(grouped.values().stream()
                .sorted(Comparator.comparing(CategorySummaryItem::totalAmount).reversed())
                .toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFactTransactions(
            LocalDate startDate,
            LocalDate endDate,
            Long accountId,
            Long categoryId,
            TransactionType type,
            TransactionSource source
    ) {
        User user = userService.currentUser();
        
        StringBuilder sql = new StringBuilder("SELECT * FROM v_fact_transactions WHERE user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(user.getId());
        
        if (startDate != null) { sql.append(" AND transaction_date >= ?"); params.add(startDate); }
        if (endDate != null) { sql.append(" AND transaction_date <= ?"); params.add(endDate); }
        if (accountId != null) { sql.append(" AND account_id = ?"); params.add(accountId); }
        if (categoryId != null) { sql.append(" AND category_id = ?"); params.add(categoryId); }
        if (type != null) { sql.append(" AND transaction_type = ?"); params.add(type.name()); }
        if (source != null) { sql.append(" AND transaction_source = ?"); params.add(source.name()); }
        
        sql.append(" ORDER BY transaction_date DESC, transaction_id DESC");
        
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    private Totals totals(List<Transaction> transactions) {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        for (Transaction tx : transactions) {
            if (tx.getType() == TransactionType.INCOME) {
                income = income.add(tx.getAmount());
            }
            if (tx.getType() == TransactionType.EXPENSE) {
                expenses = expenses.add(tx.getAmount());
            }
        }
        return new Totals(income, expenses);
    }

    private Specification<Transaction> byFilters(
            User user,
            LocalDate startDate,
            LocalDate endDate,
            Long accountId,
            Long categoryId,
            TransactionType type,
            TransactionSource source,
            TransactionStatus status
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user"), user));
            if (startDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), startDate));
            if (endDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), endDate));
            if (accountId != null) predicates.add(cb.equal(root.get("account").get("id"), accountId));
            if (categoryId != null) predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            if (type != null) predicates.add(cb.equal(root.get("type"), type));
            if (source != null) predicates.add(cb.equal(root.get("source"), source));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private record Totals(BigDecimal income, BigDecimal expenses) {
        BigDecimal net() {
            return income.subtract(expenses);
        }
    }
}
