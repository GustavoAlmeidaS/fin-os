package io.github.gustavoalmeidas.finos.ledger.dto;

import io.github.gustavoalmeidas.finos.ledger.domain.CounterpartyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCounterpartyRequest(
        @NotBlank @Size(max = 140) String name,
        @Size(max = 30) String document,
        CounterpartyType type,
        String notes
) {
}
