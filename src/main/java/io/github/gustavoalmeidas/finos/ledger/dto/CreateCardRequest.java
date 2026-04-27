package io.github.gustavoalmeidas.finos.ledger.dto;

import io.github.gustavoalmeidas.finos.ledger.domain.CardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.math.BigDecimal;

public record CreateCardRequest(
        @NotNull(message = "O id da conta é obrigatório")
        Long accountId,
        
        @NotBlank(message = "O nome do cartão é obrigatório")
        String name,
        
        @NotNull(message = "O tipo de cartão é obrigatório")
        CardType type,
        
        @Min(0)
        BigDecimal creditLimit,
        
        @Min(1) @Max(31)
        Integer closingDay,
        
        @Min(1) @Max(31)
        Integer dueDay
) {
}
