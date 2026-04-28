package io.github.gustavoalmeidas.finos.ai.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gustavoalmeidas.finos.ai.domain.AiRuleSuggestion;
import io.github.gustavoalmeidas.finos.ai.domain.SuggestionStatus;
import io.github.gustavoalmeidas.finos.ai.dto.AiResolveSuggestionRequest;
import io.github.gustavoalmeidas.finos.ai.dto.AiReviewResponseDto;
import io.github.gustavoalmeidas.finos.ai.dto.AiSuggestionResponseDto;
import io.github.gustavoalmeidas.finos.ai.infrastructure.AiRuleSuggestionRepository;
import io.github.gustavoalmeidas.finos.ai.infrastructure.OllamaClient;
import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.ledger.domain.Category;
import io.github.gustavoalmeidas.finos.ledger.domain.CategoryType;
import io.github.gustavoalmeidas.finos.ledger.domain.ClassificationRule;
import io.github.gustavoalmeidas.finos.ledger.domain.Transaction;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionType;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.CategoryRepository;
import io.github.gustavoalmeidas.finos.ledger.infrastructure.TransactionRepository;
import io.github.gustavoalmeidas.finos.ledger.repository.ClassificationRuleRepository;
import io.github.gustavoalmeidas.finos.importing.domain.ImportBatch;
import io.github.gustavoalmeidas.finos.importing.infrastructure.ImportBatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiTransactionReviewServiceTest {

    @Mock
    private OllamaClient ollamaClient;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private ImportBatchRepository batchRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private AiRuleSuggestionRepository suggestionRepository;
    @Mock
    private ClassificationRuleRepository classificationRuleRepository;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AiTransactionReviewService service;

    private User user;
    private ImportBatch batch;
    private Transaction tx;
    private Category category;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        batch = new ImportBatch();
        batch.setId(10L);

        category = new Category();
        category.setId(1L);
        category.setName("Pagamento de Fatura");
        category.setType(CategoryType.TRANSFER);

        tx = new Transaction();
        tx.setId(100L);
        tx.setRawDescription("Pagamento recebido");
        tx.setSearchableDescription("pagamento recebido");
        tx.setType(TransactionType.INCOME);
        tx.setAmount(new BigDecimal("100.00"));
        tx.setTransactionDate(LocalDate.now());
    }

    @Test
    void shouldResolveSuggestionAsApprovedAndCreateRuleWithoutOverride() {
        UUID suggestionId = UUID.randomUUID();
        AiRuleSuggestion suggestion = new AiRuleSuggestion();
        suggestion.setId(suggestionId);
        suggestion.setUser(user);
        suggestion.setKeyword("uber");
        suggestion.setSuggestedCategory(category);
        suggestion.setStatus(SuggestionStatus.PENDING);

        when(suggestionRepository.findByIdAndUser(suggestionId, user)).thenReturn(Optional.of(suggestion));

        AiResolveSuggestionRequest request = new AiResolveSuggestionRequest();
        request.setApproved(true);
        // no override
        
        service.resolveSuggestion(suggestionId, request, user);

        assertEquals(SuggestionStatus.APPROVED, suggestion.getStatus());
        assertNotNull(suggestion.getResolvedAt());
        verify(suggestionRepository).save(suggestion);

        ArgumentCaptor<ClassificationRule> ruleCaptor = ArgumentCaptor.forClass(ClassificationRule.class);
        verify(classificationRuleRepository).save(ruleCaptor.capture());
        assertEquals("uber", ruleCaptor.getValue().getKeyword());
        assertEquals(category, ruleCaptor.getValue().getCategory());
        assertTrue(ruleCaptor.getValue().isActive());
        
        // ensure transactions were NOT updated (only done if override provided)
        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    void shouldResolveSuggestionWithOverrideAndCorrectAccounting() {
        UUID suggestionId = UUID.randomUUID();
        AiRuleSuggestion suggestion = new AiRuleSuggestion();
        suggestion.setId(suggestionId);
        suggestion.setUser(user);
        suggestion.setKeyword("pagamento recebido");
        suggestion.setSuggestedCategory(new Category()); // original bad suggestion
        suggestion.getSuggestedCategory().setId(99L);
        suggestion.setStatus(SuggestionStatus.PENDING);
        suggestion.setImportBatch(batch);

        when(suggestionRepository.findByIdAndUser(suggestionId, user)).thenReturn(Optional.of(suggestion));
        when(categoryRepository.findByIdAndUser(2L, user)).thenReturn(Optional.of(category)); // new category TRANSFER
        when(transactionRepository.findByImportBatchId(batch.getId())).thenReturn(List.of(tx));

        AiResolveSuggestionRequest request = new AiResolveSuggestionRequest();
        request.setApproved(true);
        request.setOverrideCategoryId(2L); // override to the correct transfer category
        
        service.resolveSuggestion(suggestionId, request, user);

        assertEquals(SuggestionStatus.APPROVED, suggestion.getStatus());
        
        // Verify transaction was updated
        assertEquals(category, tx.getCategory());
        assertEquals(TransactionType.TRANSFER, tx.getType()); // Should force to TRANSFER because CategoryType is TRANSFER
        verify(transactionRepository).saveAll(List.of(tx));

        // Verify rule was created with the override category
        ArgumentCaptor<ClassificationRule> ruleCaptor = ArgumentCaptor.forClass(ClassificationRule.class);
        verify(classificationRuleRepository).save(ruleCaptor.capture());
        assertEquals("pagamento recebido", ruleCaptor.getValue().getKeyword());
        assertEquals(category, ruleCaptor.getValue().getCategory());
    }

    @Test
    void shouldResolveSuggestionAsRejected() {
        UUID suggestionId = UUID.randomUUID();
        AiRuleSuggestion suggestion = new AiRuleSuggestion();
        suggestion.setId(suggestionId);
        suggestion.setUser(user);
        suggestion.setStatus(SuggestionStatus.PENDING);

        when(suggestionRepository.findByIdAndUser(suggestionId, user)).thenReturn(Optional.of(suggestion));

        AiResolveSuggestionRequest request = new AiResolveSuggestionRequest();
        request.setApproved(false);
        
        service.resolveSuggestion(suggestionId, request, user);

        assertEquals(SuggestionStatus.REJECTED, suggestion.getStatus());
        verify(suggestionRepository).save(suggestion);
        verify(classificationRuleRepository, never()).save(any());
        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    void shouldEnrichPendingSuggestionsWithSampleTransactionData() {
        UUID suggestionId = UUID.randomUUID();
        AiRuleSuggestion suggestion = new AiRuleSuggestion();
        suggestion.setId(suggestionId);
        suggestion.setKeyword("recebido");
        suggestion.setSuggestedCategory(category);
        suggestion.setStatus(SuggestionStatus.PENDING);
        suggestion.setImportBatch(batch);
        
        when(suggestionRepository.findByUserAndStatusOrderByCreatedAtDesc(user, SuggestionStatus.PENDING))
            .thenReturn(List.of(suggestion));
            
        when(transactionRepository.findByImportBatchId(batch.getId()))
            .thenReturn(List.of(tx));
            
        List<AiSuggestionResponseDto> dtos = service.listPendingSuggestions(user);
        
        assertEquals(1, dtos.size());
        AiSuggestionResponseDto dto = dtos.get(0);
        assertEquals("recebido", dto.getKeyword());
        assertEquals(category.getName(), dto.getSuggestedCategoryName());
        
        // Enrichment validation
        assertEquals("Pagamento recebido", dto.getSampleRawDescription());
        assertEquals(new BigDecimal("100.00"), dto.getSampleAmount());
        assertEquals(LocalDate.now(), dto.getSampleDate());
    }
}
