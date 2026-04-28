package io.github.gustavoalmeidas.finos.ai.infrastructure;

import io.github.gustavoalmeidas.finos.ai.domain.AiRuleSuggestion;
import io.github.gustavoalmeidas.finos.ai.domain.SuggestionStatus;
import io.github.gustavoalmeidas.finos.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiRuleSuggestionRepository extends JpaRepository<AiRuleSuggestion, UUID> {
    
    List<AiRuleSuggestion> findByUserAndStatusOrderByCreatedAtDesc(User user, SuggestionStatus status);

    Optional<AiRuleSuggestion> findByIdAndUser(UUID id, User user);
    
    boolean existsByUserAndKeywordAndStatus(User user, String keyword, SuggestionStatus status);
}
