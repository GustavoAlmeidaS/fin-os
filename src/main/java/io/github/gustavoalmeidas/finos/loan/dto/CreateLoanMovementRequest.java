package io.github.gustavoalmeidas.finos.loan.dto;

import io.github.gustavoalmeidas.finos.loan.domain.LoanMovementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateLoanMovementRequest(
        @NotNull LoanMovementType type,
        @NotNull BigDecimal amount,
        @NotNull LocalDate movementDate,
        @NotBlank String description,
        Long transactionId
) {}
