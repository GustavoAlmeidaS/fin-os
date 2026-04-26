package io.github.gustavoalmeidas.finos.loan.api;

import io.github.gustavoalmeidas.finos.loan.application.LoanService;
import io.github.gustavoalmeidas.finos.loan.dto.CreateLoanRequest;
import io.github.gustavoalmeidas.finos.loan.dto.LoanResponse;
import io.github.gustavoalmeidas.finos.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    @GetMapping
    public ApiResponse<List<LoanResponse>> list() {
        return ApiResponse.ok(loanService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<LoanResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(loanService.get(id));
    }

    @PostMapping
    public ApiResponse<LoanResponse> create(@RequestBody CreateLoanRequest request) {
        return ApiResponse.ok(loanService.create(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        loanService.delete(id);
        return ApiResponse.ok(null);
    }
}
