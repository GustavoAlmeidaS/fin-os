package io.github.gustavoalmeidas.finos.ai.api;

import io.github.gustavoalmeidas.finos.ai.application.AiAdvisorService;
import io.github.gustavoalmeidas.finos.ai.application.AiTransactionReviewService;
import io.github.gustavoalmeidas.finos.ai.dto.AiChatRequest;
import io.github.gustavoalmeidas.finos.ai.dto.AiInsightsResponse;
import io.github.gustavoalmeidas.finos.ai.dto.AiReviewSummary;
import io.github.gustavoalmeidas.finos.ai.dto.AiSuggestionResponseDto;
import io.github.gustavoalmeidas.finos.ai.dto.AiResolveSuggestionRequest;
import io.github.gustavoalmeidas.finos.shared.api.ApiResponse;
import io.github.gustavoalmeidas.finos.identity.application.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiAdvisorService aiAdvisorService;
    private final AiTransactionReviewService aiReviewService;
    private final UserService userService;

    @GetMapping("/insights")
    public ApiResponse<AiInsightsResponse> getGeneralInsights() {
        String advice = aiAdvisorService.generateGeneralInsights();
        return ApiResponse.ok(new AiInsightsResponse(advice));
    }

    @PostMapping("/chat")
    public ApiResponse<AiInsightsResponse> chat(@Valid @RequestBody AiChatRequest request) {
        String response = aiAdvisorService.askQuestion(request.getQuestion());
        return ApiResponse.ok(new AiInsightsResponse(response));
    }

    @PostMapping("/review-batch/{batchId}")
    public ApiResponse<AiReviewSummary> reviewBatch(@PathVariable Long batchId) {
        try {
            AiReviewSummary summary = aiReviewService.reviewBatch(batchId, userService.currentUser());
            return ApiResponse.ok("Revisão concluída", summary);
        } catch (Exception e) {
            return ApiResponse.<AiReviewSummary>builder()
                    .success(false)
                    .message("Erro na revisão IA: " + e.getMessage())
                    .build();
        }
    }

    @GetMapping("/suggestions")
    public ApiResponse<java.util.List<AiSuggestionResponseDto>> listSuggestions() {
        return ApiResponse.ok(aiReviewService.listPendingSuggestions(userService.currentUser()));
    }

    @PostMapping("/suggestions/{id}/resolve")
    public ApiResponse<Void> resolveSuggestion(@PathVariable java.util.UUID id, @RequestBody AiResolveSuggestionRequest request) {
        aiReviewService.resolveSuggestion(id, request, userService.currentUser());
        return ApiResponse.ok(null);
    }
}
