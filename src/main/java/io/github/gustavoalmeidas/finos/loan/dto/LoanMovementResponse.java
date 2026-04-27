package io.github.gustavoalmeidas.finos.loan.dto;

import io.github.gustavoalmeidas.finos.loan.domain.LoanMovementType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanMovementResponse(
        Long id,
        Long loanId,
        Long transactionId,
        LoanMovementType type,
        BigDecimal amount,
        LocalDate movementDate,
        String description
) {}
