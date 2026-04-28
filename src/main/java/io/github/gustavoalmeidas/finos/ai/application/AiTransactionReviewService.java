package io.github.gustavoalmeidas.finos.ai.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavoalmeidas.finos.ai.domain.AiRuleSuggestion;
import io.github.gustavoalmeidas.finos.ai.domain.SuggestionStatus;
import io.github.gustavoalmeidas.finos.ai.dto.AiReviewResponseDto;
import io.github.gustavoalmeidas.finos.ai.dto.AiReviewSummary;
import io.github.gustavoalmeidas.finos.ai.dto.AiSuggestionResponseDto;
import io.github.gustavoalmeidas.finos.ai.infrastructure.AiRuleSuggestionRepository;
import io.github.gustavoalmeidas.finos.ai.infrastructure.OllamaClient;
import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.ledger.domain.Category;
import io.github.gustavoalmeidas.finos.ledger.domain.Transaction;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionType;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.CategoryRepository;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.TransactionRepository;
import io.github.gustavoalmeidas.finos.ledger.repository.ClassificationRuleRepository;
import io.github.gustavoalmeidas.finos.importing.domain.ImportBatch;
import io.github.gustavoalmeidas.finos.importing.infrastructure.ImportBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiTransactionReviewService {

    private final OllamaClient ollamaClient;
    private final TransactionRepository transactionRepository;
    private final ImportBatchRepository batchRepository;
    private final CategoryRepository categoryRepository;
    private final AiRuleSuggestionRepository suggestionRepository;
    private final ClassificationRuleRepository classificationRuleRepository;
    private final ObjectMapper objectMapper;

    private static final int BATCH_SIZE = 15;

    /**
     * Builds the system prompt dynamically, injecting the user's existing categories
     * so the AI knows which names are valid.
     */
    private String buildSystemPrompt(User user) {
        List<Category> userCategories = categoryRepository.findByUserOrderByTypeAscNameAsc(user);
        String categoryList;
        if (userCategories.isEmpty()) {
            categoryList = "O usuário não tem categorias cadastradas. Sugira nomes descritivos curtos.";
        } else {
            categoryList = userCategories.stream()
                    .map(c -> "- " + c.getName() + " (" + c.getType().name() + ")")
                    .collect(Collectors.joining("\n"));
        }

        return """
            Você é um assistente de ETL e reclassificação financeira.
            Sua função é receber uma lista de transações bancárias recém importadas e corrigir categorias e tipos, devolvendo um JSON.
            
            === CATEGORIAS DISPONÍVEIS DO USUÁRIO ===
            %s
            ==========================================
            
            REGRAS OBRIGATÓRIAS:
            1. Você DEVE usar SOMENTE os nomes de categoria listados acima. NÃO invente categorias novas.
            2. Se nenhuma categoria existente se encaixa, NÃO inclua a transação na resposta.
            3. O JSON deve ter este formato EXATO para cada item sugerido:
            [
              {
                "transactionId": 123,
                "suggestedType": "TRANSFER",
                "suggestedCategory": "Nome EXATO da Categoria da lista acima",
                "keywordToLearn": "pagamento fatura",
                "justification": "Breve justificativa"
              }
            ]
            4. A propriedade "keywordToLearn" deve ser a melhor palavra-chave curta (1 a 3 palavras) extraída da descrição original.
            5. Se a descrição indicar pagamento de cartão de crédito (ex: "Pagamento recebido"), o tipo deve ser TRANSFER.
            6. Se nenhuma transação precisa de correção, retorne um array vazio: []
            7. O retorno DEVE ser um JSON válido e nada mais. Não inclua Markdown, backticks ou texto extra.
            """.formatted(categoryList);
    }

    /**
     * Reviews a batch of transactions using AI. NOT wrapped in @Transactional
     * since it calls an external service (Ollama) that can take many minutes.
     * Each sub-batch saves its own results in a separate transaction.
     */
    public AiReviewSummary reviewBatch(Long batchId, User user) {
        ImportBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Lote de importação não encontrado"));

        List<Transaction> allTransactions = transactionRepository.findByImportBatchId(batchId);
        if (allTransactions.isEmpty()) {
            return AiReviewSummary.builder()
                    .totalTransactions(0)
                    .status("NO_SUGGESTIONS")
                    .message("Nenhuma transação encontrada neste lote.")
                    .build();
        }

        // Build the system prompt with the user's actual categories
        String systemPrompt = buildSystemPrompt(user);

        int totalSuggestionsCreated = 0;
        int totalTxUpdated = 0;
        int totalCategoriesNotFound = 0;
        int totalParseErrors = 0;

        List<List<Transaction>> subBatches = partition(allTransactions, BATCH_SIZE);
        log.info("Starting AI review for batch {} with {} transactions ({} sub-batches)", batchId, allTransactions.size(), subBatches.size());

        for (int i = 0; i < subBatches.size(); i++) {
            List<Transaction> subBatch = subBatches.get(i);
            log.info("Processing sub-batch {}/{} ({} transactions)", i + 1, subBatches.size(), subBatch.size());

            try {
                StringBuilder promptBuilder = new StringBuilder("TRANSAÇÕES PARA REVISÃO:\n");
                for (Transaction tx : subBatch) {
                    promptBuilder.append(String.format("ID: %d | Data: %s | Descrição: %s | Valor: %s | Tipo: %s | CategoriaAtual: %s\n",
                            tx.getId(), tx.getTransactionDate(), tx.getRawDescription(), tx.getAmount(), tx.getType(),
                            (tx.getCategory() != null ? tx.getCategory().getName() : "NENHUMA")));
                }

                String jsonResponse = ollamaClient.generateJson(systemPrompt, promptBuilder.toString());
                log.info("Ollama sub-batch {}/{} response: {}", i + 1, subBatches.size(), jsonResponse);

                List<AiReviewResponseDto> suggestions;
                try {
                    suggestions = objectMapper.readValue(jsonResponse, new TypeReference<List<AiReviewResponseDto>>() {});
                } catch (Exception parseEx) {
                    log.warn("Failed to parse Ollama response for sub-batch {}: {}. Raw: {}", i + 1, parseEx.getMessage(), jsonResponse);
                    totalParseErrors++;
                    continue;
                }

                if (suggestions == null || suggestions.isEmpty()) {
                    log.info("No suggestions from AI for sub-batch {}", i + 1);
                    continue;
                }

                // Save results for this sub-batch in its own transaction
                int[] results = saveSubBatchResults(suggestions, subBatch, batch, user);
                totalTxUpdated += results[0];
                totalSuggestionsCreated += results[1];
                totalCategoriesNotFound += results[2];

            } catch (Exception e) {
                log.error("Error during AI review sub-batch {}/{}: {}", i + 1, subBatches.size(), e.getMessage(), e);
                totalParseErrors++;
            }
        }

        log.info("AI review completed for batch {}: {} tx updated, {} suggestions created, {} categories not found, {} parse errors",
                batchId, totalTxUpdated, totalSuggestionsCreated, totalCategoriesNotFound, totalParseErrors);

        String status = totalSuggestionsCreated > 0 ? "SUCCESS" : (totalTxUpdated > 0 ? "PARTIAL" : "NO_SUGGESTIONS");
        String message;
        if (totalSuggestionsCreated > 0) {
            message = String.format("A IA analisou %d transações e criou %d sugestão(ões) de regras para sua aprovação.",
                    allTransactions.size(), totalSuggestionsCreated);
        } else if (totalCategoriesNotFound > 0) {
            message = String.format("A IA sugeriu categorias, mas %d não foram encontradas no seu cadastro. Crie as categorias necessárias e tente novamente.",
                    totalCategoriesNotFound);
        } else {
            message = "A IA não encontrou correções necessárias para este lote.";
        }

        return AiReviewSummary.builder()
                .totalTransactions(allTransactions.size())
                .subBatchesProcessed(subBatches.size())
                .suggestionsCreated(totalSuggestionsCreated)
                .transactionsUpdated(totalTxUpdated)
                .categoriesNotFound(totalCategoriesNotFound)
                .parseErrors(totalParseErrors)
                .status(status)
                .message(message)
                .build();
    }

    /**
     * Saves the AI results for one sub-batch in its own transaction scope.
     * Returns [txUpdated, suggestionsCreated, categoriesNotFound].
     */
    @Transactional
    public int[] saveSubBatchResults(List<AiReviewResponseDto> suggestions, List<Transaction> subBatch, ImportBatch batch, User user) {
        int txUpdated = 0;
        int suggestionsCreated = 0;
        int categoriesNotFound = 0;

        for (AiReviewResponseDto suggestion : suggestions) {
            if (suggestion.getTransactionId() == null) continue;

            Optional<Transaction> txOpt = subBatch.stream()
                    .filter(t -> t.getId().equals(suggestion.getTransactionId()))
                    .findFirst();
            if (txOpt.isEmpty()) continue;

            Transaction tx = txOpt.get();

            if (suggestion.getSuggestedCategory() != null) {
                Category cat = categoryRepository.findFirstByUserAndNameIgnoreCase(user, suggestion.getSuggestedCategory().trim()).orElse(null);
                if (cat != null) {
                    tx.setCategory(cat);
                    if (suggestion.getSuggestedType() != null) {
                        try {
                            tx.setType(TransactionType.valueOf(suggestion.getSuggestedType()));
                        } catch (Exception ignored) {}
                    }
                    transactionRepository.save(tx);
                    txUpdated++;

                    String keyword = suggestion.getKeywordToLearn();
                    if (keyword != null && !keyword.isBlank()) {
                        keyword = keyword.trim().toLowerCase();
                        boolean exists = suggestionRepository.existsByUserAndKeywordAndStatus(user, keyword, SuggestionStatus.PENDING);
                        if (!exists) {
                            AiRuleSuggestion aiRule = new AiRuleSuggestion();
                            aiRule.setUser(user);
                            aiRule.setKeyword(keyword);
                            aiRule.setSuggestedCategory(cat);
                            aiRule.setStatus(SuggestionStatus.PENDING);
                            aiRule.setImportBatch(batch);
                            suggestionRepository.save(aiRule);
                            suggestionsCreated++;
                            log.info("Created PENDING suggestion: keyword='{}', category='{}'", keyword, cat.getName());
                        }
                    }
                } else {
                    categoriesNotFound++;
                    log.warn("Category '{}' suggested by AI not found for user {}", suggestion.getSuggestedCategory(), user.getId());
                }
            }
        }

        return new int[]{txUpdated, suggestionsCreated, categoriesNotFound};
    }

    @Transactional(readOnly = true)
    public List<AiSuggestionResponseDto> listPendingSuggestions(User user) {
        return suggestionRepository.findByUserAndStatusOrderByCreatedAtDesc(user, SuggestionStatus.PENDING)
                .stream()
                .map(s -> {
                    AiSuggestionResponseDto dto = new AiSuggestionResponseDto();
                    dto.setId(s.getId());
                    dto.setKeyword(s.getKeyword());
                    dto.setSuggestedCategoryId(s.getSuggestedCategory().getId());
                    dto.setSuggestedCategoryName(s.getSuggestedCategory().getName());
                    dto.setStatus(s.getStatus());
                    dto.setCreatedAt(s.getCreatedAt());
                    
                    // Fetch a sample transaction to provide context to the user
                    if (s.getImportBatch() != null) {
                        transactionRepository.findByImportBatchId(s.getImportBatch().getId()).stream()
                            .filter(t -> t.getSearchableDescription() != null && t.getSearchableDescription().contains(s.getKeyword()))
                            .findFirst()
                            .ifPresent(tx -> {
                                dto.setSampleRawDescription(tx.getRawDescription());
                                dto.setSampleAmount(tx.getAmount());
                                dto.setSampleDate(tx.getTransactionDate());
                            });
                    }
                    return dto;
                }).toList();
    }

    @Transactional
    public void resolveSuggestion(java.util.UUID id, io.github.gustavoalmeidas.finos.ai.dto.AiResolveSuggestionRequest request, User user) {
        AiRuleSuggestion suggestion = suggestionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Sugestão não encontrada"));

        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new IllegalStateException("Sugestão já foi resolvida.");
        }

        suggestion.setResolvedAt(java.time.LocalDateTime.now());

        if (request.isApproved()) {
            suggestion.setStatus(SuggestionStatus.APPROVED);
            
            Category finalCategory = suggestion.getSuggestedCategory();
            
            if (request.getOverrideCategoryId() != null && !request.getOverrideCategoryId().equals(suggestion.getSuggestedCategory().getId())) {
                finalCategory = categoryRepository.findByIdAndUser(request.getOverrideCategoryId(), user)
                        .orElseThrow(() -> new IllegalArgumentException("Categoria de substituição não encontrada."));
                
                if (suggestion.getImportBatch() != null) {
                    List<Transaction> transactionsToUpdate = transactionRepository.findByImportBatchId(suggestion.getImportBatch().getId())
                            .stream()
                            .filter(t -> t.getSearchableDescription() != null && t.getSearchableDescription().contains(suggestion.getKeyword()))
                            .toList();
                            
                    for (Transaction tx : transactionsToUpdate) {
                        tx.setCategory(finalCategory);
                        if (finalCategory.getType() == io.github.gustavoalmeidas.finos.ledger.domain.CategoryType.TRANSFER ||
                            finalCategory.getType() == io.github.gustavoalmeidas.finos.ledger.domain.CategoryType.ADJUSTMENT) {
                            tx.setType(TransactionType.valueOf(finalCategory.getType().name()));
                        }
                    }
                    transactionRepository.saveAll(transactionsToUpdate);
                }
            }
            
            io.github.gustavoalmeidas.finos.ledger.domain.ClassificationRule rule = new io.github.gustavoalmeidas.finos.ledger.domain.ClassificationRule();
            rule.setUser(user);
            rule.setKeyword(suggestion.getKeyword());
            rule.setCategory(finalCategory);
            rule.setActive(true);
            classificationRuleRepository.save(rule);
        } else {
            suggestion.setStatus(SuggestionStatus.REJECTED);
        }

        suggestionRepository.save(suggestion);
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}
