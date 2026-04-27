package io.github.gustavoalmeidas.finos.planning.api;

import io.github.gustavoalmeidas.finos.planning.application.PlanningService;
import io.github.gustavoalmeidas.finos.planning.dto.CategoryBudgetResponse;
import io.github.gustavoalmeidas.finos.planning.dto.CreateCategoryBudgetRequest;
import io.github.gustavoalmeidas.finos.planning.dto.CreateGoalRequest;
import io.github.gustavoalmeidas.finos.planning.dto.GoalResponse;
import io.github.gustavoalmeidas.finos.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/planning")
public class PlanningController {

    private final PlanningService planningService;

    @GetMapping("/goals")
    public ApiResponse<List<GoalResponse>> listGoals() {
        return ApiResponse.ok(planningService.listGoals());
    }

    @PostMapping("/goals")
    public ApiResponse<GoalResponse> createGoal(@RequestBody CreateGoalRequest request) {
        return ApiResponse.ok(planningService.createGoal(request));
    }

    @GetMapping("/budgets")
    public ApiResponse<List<CategoryBudgetResponse>> listBudgets() {
        return ApiResponse.ok(planningService.listBudgets());
    }

    @PostMapping("/budgets")
    public ApiResponse<CategoryBudgetResponse> createOrUpdateBudget(@RequestBody CreateCategoryBudgetRequest request) {
        return ApiResponse.ok(planningService.createOrUpdateBudget(request));
    }

    @PatchMapping("/goals/{id}/deposit")
    public ApiResponse<GoalResponse> depositToGoal(@PathVariable Long id, @jakarta.validation.Valid @RequestBody io.github.gustavoalmeidas.finos.planning.dto.DepositGoalRequest request) {
        return ApiResponse.ok("Depósito registrado com sucesso.", planningService.depositToGoal(id, request.amount()));
    }
}
