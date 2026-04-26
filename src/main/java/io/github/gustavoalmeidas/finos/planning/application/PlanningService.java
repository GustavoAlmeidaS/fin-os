package io.github.gustavoalmeidas.finos.planning.application;

import io.github.gustavoalmeidas.finos.identity.application.UserService;
import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.ledger.application.CategoryService;
import io.github.gustavoalmeidas.finos.ledger.domain.Category;
import io.github.gustavoalmeidas.finos.planning.domain.CategoryBudget;
import io.github.gustavoalmeidas.finos.planning.domain.Goal;
import io.github.gustavoalmeidas.finos.planning.dto.CategoryBudgetResponse;
import io.github.gustavoalmeidas.finos.planning.dto.CreateCategoryBudgetRequest;
import io.github.gustavoalmeidas.finos.planning.dto.CreateGoalRequest;
import io.github.gustavoalmeidas.finos.planning.dto.GoalResponse;
import io.github.gustavoalmeidas.finos.planning.infrastructure.CategoryBudgetRepository;
import io.github.gustavoalmeidas.finos.planning.infrastructure.GoalRepository;
import io.github.gustavoalmeidas.finos.planning.mapper.PlanningMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanningService {

    private final UserService userService;
    private final CategoryService categoryService;
    private final GoalRepository goalRepository;
    private final CategoryBudgetRepository budgetRepository;
    private final PlanningMapper mapper;

    @Transactional(readOnly = true)
    public List<GoalResponse> listGoals() {
        User user = userService.currentUser();
        return goalRepository.findByUserOrderByNameAsc(user).stream()
                .map(mapper::toGoalResponse)
                .toList();
    }

    @Transactional
    public GoalResponse createGoal(CreateGoalRequest request) {
        User user = userService.currentUser();
        Goal goal = new Goal();
        goal.setUser(user);
        goal.setName(request.name());
        goal.setTargetAmount(request.targetAmount());
        goal.setTargetDate(request.targetDate());
        goal.setColor(request.color());
        return mapper.toGoalResponse(goalRepository.save(goal));
    }

    @Transactional(readOnly = true)
    public List<CategoryBudgetResponse> listBudgets() {
        User user = userService.currentUser();
        return budgetRepository.findByUserAndActiveTrue(user).stream()
                .map(mapper::toBudgetResponse)
                .toList();
    }

    @Transactional
    public CategoryBudgetResponse createOrUpdateBudget(CreateCategoryBudgetRequest request) {
        User user = userService.currentUser();
        Category category = categoryService.getOwnedEntity(request.categoryId());

        CategoryBudget budget = budgetRepository.findByUserAndCategoryId(user, category.getId())
                .orElseGet(() -> {
                    CategoryBudget b = new CategoryBudget();
                    b.setUser(user);
                    b.setCategory(category);
                    return b;
                });

        budget.setAmountLimit(request.amountLimit());
        budget.setActive(true);
        return mapper.toBudgetResponse(budgetRepository.save(budget));
    }
}
