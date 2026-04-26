package io.github.gustavoalmeidas.finos.loan.mapper;

import io.github.gustavoalmeidas.finos.loan.domain.Loan;
import io.github.gustavoalmeidas.finos.loan.dto.LoanResponse;
import org.springframework.stereotype.Component;

@Component
public class LoanMapper {

    public LoanResponse toLoanResponse(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getName(),
                loan.getPrincipalAmount(),
                loan.getCurrentBalance(),
                loan.getLoanType(),
                loan.getCounterparty() != null ? loan.getCounterparty().getId() : null,
                loan.getCounterparty() != null ? loan.getCounterparty().getName() : null,
                loan.getStartDate(),
                loan.getEndDate(),
                loan.getInterestRate(),
                loan.getStatus(),
                loan.getNotes()
        );
    }
}
