package io.github.gustavoalmeidas.finos.ledger.dto;

import io.github.gustavoalmeidas.finos.ledger.domain.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull AccountType type,
        @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @DecimalMin("0.00") BigDecimal initialBalance,
        @Size(max = 120) String institutionName,
        @Pattern(regexp = "^#([A-Fa-f0-9]{6})$") String color,
        String notes
) {
}
