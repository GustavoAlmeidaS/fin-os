package io.github.gustavoalmeidas.finos.importing.application;

import io.github.gustavoalmeidas.finos.ledger.domain.Category;
import io.github.gustavoalmeidas.finos.ledger.domain.ClassificationRule;
import io.github.gustavoalmeidas.finos.ledger.repository.ClassificationRuleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TransactionClassifier {

    private final ClassificationRuleRepository ruleRepository;
    private final io.github.gustavoalmeidas.finos.ledger.infrastructure.TransactionRepository transactionRepository;
    private final io.github.gustavoalmeidas.finos.ledger.infrastructure.CategoryRepository categoryRepository;

    public TransactionClassifier(ClassificationRuleRepository ruleRepository,
                                 io.github.gustavoalmeidas.finos.ledger.infrastructure.TransactionRepository transactionRepository,
                                 io.github.gustavoalmeidas.finos.ledger.infrastructure.CategoryRepository categoryRepository) {
        this.ruleRepository = ruleRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Finds the best matching category for a sanitized description.
     */
    public Optional<Category> classify(String searchableDescription, Long userId) {
        if (searchableDescription == null || searchableDescription.isBlank()) {
            return Optional.empty();
        }

        // Ideally this list is cached per user to avoid hitting DB for every row
        List<ClassificationRule> activeRules = ruleRepository.findActiveRulesOrdered(userId);

        for (ClassificationRule rule : activeRules) {
            // Simple substring match. Since both are sanitized (lowercase), it works well.
            // A more advanced engine could use Regex or Lucene, but substring is fast and predictable.
            if (searchableDescription.contains(rule.getKeyword().toLowerCase())) {
                return Optional.of(rule.getCategory());
            }
        }

        // HISTORICAL VOTING ENGINE:
        // If no explicit rule matched, look for the most frequently used category for this exact description
        List<Long> frequentCatIds = transactionRepository.findMostFrequentCategoryIdByDescription(userId, searchableDescription);
        if (frequentCatIds != null && !frequentCatIds.isEmpty()) {
            return categoryRepository.findById(frequentCatIds.get(0));
        }

        return Optional.empty();
    }
}
