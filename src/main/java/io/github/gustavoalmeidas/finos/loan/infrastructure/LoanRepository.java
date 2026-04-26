package io.github.gustavoalmeidas.finos.loan.infrastructure;

import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.loan.domain.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUserOrderByNameAsc(User user);
    Optional<Loan> findByIdAndUser(Long id, User user);
}
