package io.github.gustavoalmeidas.finos.loan.api;

import io.github.gustavoalmeidas.finos.loan.application.LoanMovementService;
import io.github.gustavoalmeidas.finos.loan.dto.CreateLoanMovementRequest;
import io.github.gustavoalmeidas.finos.loan.dto.LoanMovementResponse;
import io.github.gustavoalmeidas.finos.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/loans/{loanId}/movements")
public class LoanMovementController {

    private final LoanMovementService loanMovementService;

    @GetMapping
    public ApiResponse<List<LoanMovementResponse>> list(@PathVariable Long loanId) {
        return ApiResponse.ok(loanMovementService.list(loanId));
    }

    @PostMapping
    public ApiResponse<LoanMovementResponse> create(
            @PathVariable Long loanId, 
            @Valid @RequestBody CreateLoanMovementRequest request) {
        return ApiResponse.ok("Movimentação de empréstimo registrada com sucesso.", loanMovementService.create(loanId, request));
    }
}
