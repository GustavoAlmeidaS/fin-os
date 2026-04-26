package io.github.gustavoalmeidas.finos.ledger.dto;

import io.github.gustavoalmeidas.finos.ledger.domain.AccountType;

import java.math.BigDecimal;

public record AccountResponse(
        Long id,
        String name,
        AccountType type,
        String currency,
        BigDecimal initialBalance,
        BigDecimal currentBalance,
        boolean active,
        String institutionName,
        String color,
        String notes
) {
}
