package io.github.gustavoalmeidas.finos.loan.application;

import io.github.gustavoalmeidas.finos.loan.domain.Loan;
import io.github.gustavoalmeidas.finos.loan.domain.LoanMovement;
import io.github.gustavoalmeidas.finos.loan.domain.LoanMovementType;
import io.github.gustavoalmeidas.finos.loan.dto.CreateLoanMovementRequest;
import io.github.gustavoalmeidas.finos.loan.dto.LoanMovementResponse;
import io.github.gustavoalmeidas.finos.loan.infrastructure.LoanMovementRepository;
import io.github.gustavoalmeidas.finos.loan.infrastructure.LoanRepository;
import io.github.gustavoalmeidas.finos.loan.mapper.LoanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanMovementService {

    private final LoanService loanService;
    private final LoanRepository loanRepository;
    private final LoanMovementRepository loanMovementRepository;
    private final LoanMapper mapper;

    @Transactional(readOnly = true)
    public List<LoanMovementResponse> list(Long loanId) {
        Loan loan = loanService.getOwnedEntity(loanId);
        return loanMovementRepository.findByLoanOrderByMovementDateDescIdDesc(loan).stream()
                .map(mapper::toLoanMovementResponse)
                .toList();
    }

    @Transactional
    public LoanMovementResponse create(Long loanId, CreateLoanMovementRequest request) {
        Loan loan = loanService.getOwnedEntity(loanId);
        
        LoanMovement movement = new LoanMovement();
        movement.setLoan(loan);
        movement.setType(request.type());
        movement.setAmount(request.amount());
        movement.setMovementDate(request.movementDate());
        movement.setDescription(request.description());

        loanMovementRepository.save(movement);

        if (request.type() == LoanMovementType.PAYMENT) {
            loan.setCurrentBalance(loan.getCurrentBalance().subtract(request.amount()));
        } else if (request.type() == LoanMovementType.DISBURSEMENT) {
            loan.setCurrentBalance(loan.getCurrentBalance().add(request.amount()));
        } else if (request.type() == LoanMovementType.ADJUSTMENT) {
            loan.setCurrentBalance(loan.getCurrentBalance().add(request.amount()));
        }
        
        loanRepository.save(loan);

        return mapper.toLoanMovementResponse(movement);
    }
}
