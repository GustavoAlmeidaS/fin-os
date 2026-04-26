package io.github.gustavoalmeidas.finos.loan.infrastructure;

import io.github.gustavoalmeidas.finos.loan.domain.Loan;
import io.github.gustavoalmeidas.finos.loan.domain.LoanMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanMovementRepository extends JpaRepository<LoanMovement, Long> {
    List<LoanMovement> findByLoanOrderByMovementDateDescIdDesc(Loan loan);
}
