package io.github.gustavoalmeidas.finos.ledger.infrastructure;

import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.ledger.domain.Category;
import io.github.gustavoalmeidas.finos.ledger.domain.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserOrderByTypeAscNameAsc(User user);

    Optional<Category> findByIdAndUser(Long id, User user);

    Optional<Category> findFirstByUserAndNameIgnoreCase(User user, String name);

    boolean existsByUserAndNameAndType(User user, String name, CategoryType type);
}
