package io.github.gustavoalmeidas.finos.ledger.repository;

import io.github.gustavoalmeidas.finos.ledger.domain.ClassificationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ClassificationRuleRepository extends JpaRepository<ClassificationRule, Long> {
    
    @Query("SELECT c FROM ClassificationRule c WHERE c.user.id = :userId AND c.active = true ORDER BY c.priority DESC, LENGTH(c.keyword) DESC")
    List<ClassificationRule> findActiveRulesOrdered(@Param("userId") Long userId);
}
