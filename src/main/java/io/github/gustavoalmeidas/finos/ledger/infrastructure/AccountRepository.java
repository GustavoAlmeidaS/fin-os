package io.github.gustavoalmeidas.finos.ledger.infrastructure;

import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.ledger.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserOrderByNameAsc(User user);

    Optional<Account> findByIdAndUser(Long id, User user);
}
