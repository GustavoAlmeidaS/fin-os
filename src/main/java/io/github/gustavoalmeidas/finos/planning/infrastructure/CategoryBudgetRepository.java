package io.github.gustavoalmeidas.finos.planning.infrastructure;

import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.planning.domain.CategoryBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryBudgetRepository extends JpaRepository<CategoryBudget, Long> {
    List<CategoryBudget> findByUserAndActiveTrue(User user);
    Optional<CategoryBudget> findByIdAndUser(Long id, User user);
    Optional<CategoryBudget> findByUserAndCategoryId(User user, Long categoryId);
}
