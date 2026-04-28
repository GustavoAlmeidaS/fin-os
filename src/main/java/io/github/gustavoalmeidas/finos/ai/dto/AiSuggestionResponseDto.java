package io.github.gustavoalmeidas.finos.ai.dto;

import io.github.gustavoalmeidas.finos.ai.domain.SuggestionStatus;
import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;

@Data
public class AiSuggestionResponseDto {
    private UUID id;
    private String keyword;
    private Long suggestedCategoryId;
    private String suggestedCategoryName;
    private SuggestionStatus status;
    private LocalDateTime createdAt;
    
    // Sample transaction context
    private String sampleRawDescription;
    private java.math.BigDecimal sampleAmount;
    private java.time.LocalDate sampleDate;
}
