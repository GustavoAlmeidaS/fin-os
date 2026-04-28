package io.github.gustavoalmeidas.finos.ai.domain;

import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.importing.domain.ImportBatch;
import io.github.gustavoalmeidas.finos.ledger.domain.Category;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_rule_suggestion")
@Getter
@Setter
public class AiRuleSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "keyword", nullable = false)
    private String keyword;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggested_category_id", nullable = false)
    private Category suggestedCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SuggestionStatus status = SuggestionStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_batch_id")
    private ImportBatch importBatch;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
