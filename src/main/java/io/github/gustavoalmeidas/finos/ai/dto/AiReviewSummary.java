package io.github.gustavoalmeidas.finos.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiReviewSummary {
    private int totalTransactions;
    private int subBatchesProcessed;
    private int suggestionsCreated;
    private int transactionsUpdated;
    private int categoriesNotFound;
    private int parseErrors;
    private String status; // "SUCCESS", "PARTIAL", "NO_SUGGESTIONS"
    private String message;
}
