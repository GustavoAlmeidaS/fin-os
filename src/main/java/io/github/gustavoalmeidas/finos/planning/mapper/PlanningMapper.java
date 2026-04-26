package io.github.gustavoalmeidas.finos.planning.mapper;

import io.github.gustavoalmeidas.finos.planning.domain.CategoryBudget;
import io.github.gustavoalmeidas.finos.planning.domain.Goal;
import io.github.gustavoalmeidas.finos.planning.dto.CategoryBudgetResponse;
import io.github.gustavoalmeidas.finos.planning.dto.GoalResponse;
import org.springframework.stereotype.Component;

@Component
public class PlanningMapper {

    public GoalResponse toGoalResponse(Goal goal) {
        return new GoalResponse(
                goal.getId(),
                goal.getName(),
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                goal.getTargetDate(),
                goal.getStatus(),
                goal.getColor()
        );
    }

    public CategoryBudgetResponse toBudgetResponse(CategoryBudget budget) {
        return new CategoryBudgetResponse(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                budget.getAmountLimit(),
                budget.isActive()
        );
    }
}
