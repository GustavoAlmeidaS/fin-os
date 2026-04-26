package io.github.gustavoalmeidas.finos.reporting.dto;

import java.util.List;

public record CategorySummaryResponse(
        List<CategorySummaryItem> categories
) {
}
