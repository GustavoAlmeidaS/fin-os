package io.github.gustavoalmeidas.finos.loan.dto;

import io.github.gustavoalmeidas.finos.loan.domain.LoanStatus;
import io.github.gustavoalmeidas.finos.loan.domain.LoanType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanResponse(
        Long id,
        String name,
        BigDecimal principalAmount,
        BigDecimal currentBalance,
        LoanType loanType,
        Long counterpartyId,
        String counterpartyName,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal interestRate,
        LoanStatus status,
        String notes
) {
}
