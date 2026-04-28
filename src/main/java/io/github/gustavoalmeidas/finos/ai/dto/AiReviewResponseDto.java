package io.github.gustavoalmeidas.finos.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiReviewResponseDto {
    private Long transactionId;
    private String suggestedType; // INCOME, EXPENSE, TRANSFER
    private String suggestedCategory;
    private String keywordToLearn;
    private String justification;
}
