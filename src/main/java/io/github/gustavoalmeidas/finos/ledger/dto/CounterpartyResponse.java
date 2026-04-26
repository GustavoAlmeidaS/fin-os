package io.github.gustavoalmeidas.finos.ledger.dto;

import io.github.gustavoalmeidas.finos.ledger.domain.CounterpartyType;

public record CounterpartyResponse(
        Long id,
        String name,
        String document,
        CounterpartyType type,
        String notes
) {
}
