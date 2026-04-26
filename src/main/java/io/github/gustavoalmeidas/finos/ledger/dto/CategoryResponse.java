package io.github.gustavoalmeidas.finos.ledger.dto;

import io.github.gustavoalmeidas.finos.ledger.domain.CategoryType;

public record CategoryResponse(
        Long id,
        String name,
        CategoryType type,
        Long parentCategoryId,
        String color,
        boolean active
) {
}
