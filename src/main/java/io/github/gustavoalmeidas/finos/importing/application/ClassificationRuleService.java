package io.github.gustavoalmeidas.finos.importing.application;

import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.ledger.domain.Category;
import io.github.gustavoalmeidas.finos.ledger.domain.ClassificationRule;
import io.github.gustavoalmeidas.finos.ledger.repository.ClassificationRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class ClassificationRuleService {

    private final ClassificationRuleRepository ruleRepository;
    private final DescriptionSanitizer sanitizer;

    // Common generic words that should NOT become rules automatically
    private static final List<String> STOP_WORDS = Arrays.asList(
            "transferencia", "pagamento", "compra", "pix", "debito", "credito",
            "recebido", "enviado", "tarifa", "taxa", "boleto", "ted", "doc",
            "cartao", "fatura", "estorno", "cancelamento", "via"
    );

    public ClassificationRuleService(ClassificationRuleRepository ruleRepository, DescriptionSanitizer sanitizer) {
        this.ruleRepository = ruleRepository;
        this.sanitizer = sanitizer;
    }

    /**
     * Learns a new rule from a user-categorized transaction.
     */
    @Transactional
    public void learnFromFeedback(String rawDescription, Category category, User user) {
        String sanitized = sanitizer.sanitize(rawDescription);
        if (sanitized.isBlank()) return;

        // Try to find the most meaningful keyword from the description
        String[] words = sanitized.split("\\s+");
        String bestKeyword = extractBestKeyword(words);

        if (bestKeyword != null) {
            // Check if rule already exists for this exact keyword and user
            boolean exists = ruleRepository.findActiveRulesOrdered(user.getId())
                    .stream()
                    .anyMatch(r -> r.getKeyword().equalsIgnoreCase(bestKeyword));

            if (!exists) {
                ClassificationRule rule = new ClassificationRule();
                rule.setUser(user);
                rule.setCategory(category);
                rule.setKeyword(bestKeyword);
                rule.setPriority(0);
                rule.setActive(true);
                rule.setCreatedFromFeedback(true);
                ruleRepository.save(rule);
            }
        }
    }

    private String extractBestKeyword(String[] words) {
        for (String word : words) {
            if (word.length() > 3 && !STOP_WORDS.contains(word)) {
                return word; // First meaningful word. More complex heuristics could be added.
            }
        }
        return null; // Could not find a safe keyword
    }
}
