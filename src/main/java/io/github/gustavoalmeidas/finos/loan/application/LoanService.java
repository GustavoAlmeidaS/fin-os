package io.github.gustavoalmeidas.finos.loan.application;

import io.github.gustavoalmeidas.finos.identity.application.UserService;
import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.ledger.application.CounterpartyService;
import io.github.gustavoalmeidas.finos.ledger.domain.Counterparty;
import io.github.gustavoalmeidas.finos.loan.domain.Loan;
import io.github.gustavoalmeidas.finos.loan.domain.LoanStatus;
import io.github.gustavoalmeidas.finos.loan.dto.CreateLoanRequest;
import io.github.gustavoalmeidas.finos.loan.dto.LoanResponse;
import io.github.gustavoalmeidas.finos.loan.infrastructure.LoanRepository;
import io.github.gustavoalmeidas.finos.loan.mapper.LoanMapper;
import io.github.gustavoalmeidas.finos.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final UserService userService;
    private final CounterpartyService counterpartyService;
    private final LoanRepository loanRepository;
    private final LoanMapper mapper;

    @Transactional(readOnly = true)
    public List<LoanResponse> list() {
        User user = userService.currentUser();
        return loanRepository.findByUserOrderByNameAsc(user).stream()
                .map(mapper::toLoanResponse)
                .toList();
    }

    @Transactional
    public LoanResponse create(CreateLoanRequest request) {
        User user = userService.currentUser();
        Loan loan = new Loan();
        loan.setUser(user);
        
        Counterparty counterparty = request.counterpartyId() != null 
                ? counterpartyService.getOwnedEntity(request.counterpartyId()) 
                : null;
        
        loan.setName(request.name());
        loan.setPrincipalAmount(request.principalAmount());
        loan.setCurrentBalance(request.principalAmount()); // Saldo inicial = Principal
        loan.setLoanType(request.loanType());
        loan.setCounterparty(counterparty);
        loan.setStartDate(request.startDate());
        loan.setEndDate(request.endDate());
        loan.setInterestRate(request.interestRate());
        loan.setNotes(request.notes());
        loan.setStatus(LoanStatus.ACTIVE);
        
        return mapper.toLoanResponse(loanRepository.save(loan));
    }

    @Transactional(readOnly = true)
    public Loan getOwnedEntity(Long id) {
        User user = userService.currentUser();
        return loanRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("loan.not-found", "Empréstimo não encontrado."));
    }

    @Transactional(readOnly = true)
    public LoanResponse get(Long id) {
        return mapper.toLoanResponse(getOwnedEntity(id));
    }

    @Transactional
    public void delete(Long id) {
        Loan loan = getOwnedEntity(id);
        loan.setDeletedAt(LocalDateTime.now());
        loanRepository.save(loan);
    }
}
