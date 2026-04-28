package io.github.gustavoalmeidas.finos.ai.dto;

import lombok.Data;

@Data
public class AiResolveSuggestionRequest {
    private boolean approved;
    private Long overrideCategoryId;
}
