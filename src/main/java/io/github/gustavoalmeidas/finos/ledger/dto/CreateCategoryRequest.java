package io.github.gustavoalmeidas.finos.ledger.dto;

import io.github.gustavoalmeidas.finos.ledger.domain.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull CategoryType type,
        Long parentCategoryId,
        @Pattern(regexp = "^#([A-Fa-f0-9]{6})$") String color
) {
}
