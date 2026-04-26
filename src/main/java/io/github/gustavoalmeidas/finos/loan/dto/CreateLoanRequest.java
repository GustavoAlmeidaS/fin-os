package io.github.gustavoalmeidas.finos.loan.dto;

import io.github.gustavoalmeidas.finos.loan.domain.LoanType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateLoanRequest(
        String name,
        BigDecimal principalAmount,
        LoanType loanType,
        Long counterpartyId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal interestRate,
        String notes
) {
}
