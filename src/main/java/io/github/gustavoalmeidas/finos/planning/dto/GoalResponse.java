package io.github.gustavoalmeidas.finos.planning.dto;

import io.github.gustavoalmeidas.finos.planning.domain.GoalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalResponse(
        Long id,
        String name,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        LocalDate targetDate,
        GoalStatus status,
        String color
) {
}
