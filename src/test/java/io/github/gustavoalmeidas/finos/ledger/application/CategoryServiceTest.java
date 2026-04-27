package io.github.gustavoalmeidas.finos.ledger.application;

import io.github.gustavoalmeidas.finos.identity.application.UserService;
import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.ledger.domain.Category;
import io.github.gustavoalmeidas.finos.ledger.domain.CategoryType;
import io.github.gustavoalmeidas.finos.ledger.dto.CategoryResponse;
import io.github.gustavoalmeidas.finos.ledger.dto.CreateCategoryRequest;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.CategoryRepository;
import io.github.gustavoalmeidas.finos.ledger.mapper.LedgerMapper;
import io.github.gustavoalmeidas.finos.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private CategoryRepository categoryRepository;

    private final LedgerMapper mapper = new LedgerMapper();

    private CategoryService service;

    @BeforeEach
    void setUp() {
        service = new CategoryService(userService, categoryRepository, mapper);
    }

    @Test
    void createRejectsDuplicateCategoryForSameUserAndType() {
        User user = user();
        when(userService.currentUser()).thenReturn(user);
        when(categoryRepository.existsByUserAndNameAndType(user, "Alimentacao", CategoryType.EXPENSE))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateCategoryRequest(
                "Alimentacao",
                CategoryType.EXPENSE,
                null,
                "#d99f3f"
        ))).isInstanceOf(BusinessException.class);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void createLinksParentCategoryOwnedByCurrentUser() {
        User user = user();
        Category parent = category(2L, user, "Moradia", CategoryType.EXPENSE);
        when(userService.currentUser()).thenReturn(user);
        when(categoryRepository.existsByUserAndNameAndType(user, "Condominio", CategoryType.EXPENSE))
                .thenReturn(false);
        when(categoryRepository.findByIdAndUser(2L, user)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(3L);
            return category;
        });

        CategoryResponse response = service.create(new CreateCategoryRequest(
                "Condominio",
                CategoryType.EXPENSE,
                2L,
                "#386b8f"
        ));

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.parentCategoryId()).isEqualTo(2L);
    }

    @Test
    void createDefaultCategoriesCreatesMissingDefaults() {
        User user = user();
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createDefaultCategories(user);

        verify(categoryRepository, times(6)).save(any(Category.class));
    }

    private static User user() {
        User user = new User();
        user.setId(1L);
        user.setUsername("gustavo");
        user.setEmail("gustavo@example.com");
        return user;
    }

    private static Category category(Long id, User user, String name, CategoryType type) {
        Category category = new Category();
        category.setId(id);
        category.setUser(user);
        category.setName(name);
        category.setType(type);
        category.setColor("#386b8f");
        return category;
    }
}
