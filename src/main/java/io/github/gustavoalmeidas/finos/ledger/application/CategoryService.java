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
import io.github.gustavoalmeidas.finos.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    @Lazy
    private final UserService userService;
    private final CategoryRepository categoryRepository;
    private final LedgerMapper mapper;

    @Transactional
    public void createDefaultCategories(User user) {
        createDefaultCategory(user, "Salário", CategoryType.INCOME, "#2f6f5e");
        createDefaultCategory(user, "Alimentação", CategoryType.EXPENSE, "#d99f3f");
        createDefaultCategory(user, "Moradia", CategoryType.EXPENSE, "#386b8f");
        createDefaultCategory(user, "Transporte", CategoryType.EXPENSE, "#c65a44");
        createDefaultCategory(user, "Transferências", CategoryType.TRANSFER, "#697a72");
        createDefaultCategory(user, "Ajustes de saldo", CategoryType.ADJUSTMENT, "#17211d");
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        User user = userService.currentUser();
        return categoryRepository.findByUserOrderByTypeAscNameAsc(user)
                .stream()
                .map(mapper::toCategoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Category getOwnedEntity(Long id) {
        User user = userService.currentUser();
        return categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("ledger.category.not-found", "Categoria não encontrada."));
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(Long id) {
        return mapper.toCategoryResponse(getOwnedEntity(id));
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        User user = userService.currentUser();
        validateUnique(user, request.name(), request.type());
        Category category = new Category();
        category.setUser(user);
        applyRequest(category, request);
        return mapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CreateCategoryRequest request) {
        Category category = getOwnedEntity(id);
        if ((!category.getName().equals(request.name()) || category.getType() != request.type())
                && categoryRepository.existsByUserAndNameAndType(category.getUser(), request.name(), request.type())) {
            throw new BusinessException("ledger.category.already_exists", "Já existe uma categoria com esse nome e tipo.");
        }
        applyRequest(category, request);
        return mapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = getOwnedEntity(id);
        category.setActive(false);
        category.setDeletedAt(java.time.LocalDateTime.now());
        categoryRepository.save(category);
    }

    private void applyRequest(Category category, CreateCategoryRequest request) {
        category.setName(request.name());
        category.setType(request.type());
        category.setColor(request.color());
        if (request.parentCategoryId() != null) {
            Category parent = getOwnedEntity(request.parentCategoryId());
            category.setParentCategory(parent);
        } else {
            category.setParentCategory(null);
        }
    }

    private void validateUnique(User user, String name, CategoryType type) {
        if (categoryRepository.existsByUserAndNameAndType(user, name, type)) {
            throw new BusinessException("ledger.category.already_exists", "Já existe uma categoria com esse nome e tipo.");
        }
    }

    private void createDefaultCategory(User user, String name, CategoryType type, String color) {
        if (categoryRepository.existsByUserAndNameAndType(user, name, type)) {
            return;
        }
        Category category = new Category();
        category.setUser(user);
        category.setName(name);
        category.setType(type);
        category.setColor(color);
        categoryRepository.save(category);
    }

    @Transactional
    public Category findOrCreateImportCategory(User user, CategoryType type) {
        String name = type == CategoryType.INCOME ? "Importações - Receitas" : "Importações - Despesas";
        return categoryRepository.findByUserOrderByTypeAscNameAsc(user).stream()
                .filter(category -> category.getType() == type && category.getName().equals(name))
                .findFirst()
                .orElseGet(() -> {
                    Category category = new Category();
                    category.setUser(user);
                    category.setName(name);
                    category.setType(type);
                    category.setColor(type == CategoryType.INCOME ? "#2f6f5e" : "#c65a44");
                    return categoryRepository.save(category);
                });
    }
}
