package io.github.gustavoalmeidas.finos.ledger.dto;

import io.github.gustavoalmeidas.finos.ledger.domain.CardType;

import java.math.BigDecimal;

public record CardResponse(
        Long id,
        Long accountId,
        String name,
        CardType type,
        BigDecimal creditLimit,
        Integer closingDay,
        Integer dueDay,
        boolean active
) {
}
