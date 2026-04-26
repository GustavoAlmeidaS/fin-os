package io.github.gustavoalmeidas.finos.planning.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateGoalRequest(
        String name,
        BigDecimal targetAmount,
        LocalDate targetDate,
        String color
) {
}
