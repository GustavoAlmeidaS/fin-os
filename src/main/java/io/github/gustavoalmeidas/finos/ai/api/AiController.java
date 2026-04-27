package io.github.gustavoalmeidas.finos.ai.api;

import io.github.gustavoalmeidas.finos.ai.application.AiAdvisorService;
import io.github.gustavoalmeidas.finos.ai.dto.AiChatRequest;
import io.github.gustavoalmeidas.finos.ai.dto.AiInsightsResponse;
import io.github.gustavoalmeidas.finos.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiAdvisorService aiAdvisorService;

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
}
