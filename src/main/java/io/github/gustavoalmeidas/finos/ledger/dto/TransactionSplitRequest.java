package io.github.gustavoalmeidas.finos.ledger.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransactionSplitRequest(
        Long categoryId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @Size(max = 255) String description
) {
}
