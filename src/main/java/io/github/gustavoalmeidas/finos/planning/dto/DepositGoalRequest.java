package io.github.gustavoalmeidas.finos.planning.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record DepositGoalRequest(
        @NotNull @Positive BigDecimal amount
) {
}
