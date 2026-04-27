package io.github.gustavoalmeidas.finos.ledger.infrastructure;

import io.github.gustavoalmeidas.finos.ledger.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByAccountId(Long accountId);
}
