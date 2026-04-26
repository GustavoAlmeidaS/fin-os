package io.github.gustavoalmeidas.finos.ledger.api;

import io.github.gustavoalmeidas.finos.ledger.application.AccountService;
import io.github.gustavoalmeidas.finos.ledger.dto.AccountResponse;
import io.github.gustavoalmeidas.finos.ledger.dto.CreateAccountRequest;
import io.github.gustavoalmeidas.finos.shared.api.ApiResponse;
import io.github.gustavoalmeidas.finos.shared.i18n.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final MessageService messageService;

    @GetMapping
    public ApiResponse<List<AccountResponse>> list() {
        return ApiResponse.ok(accountService.list());
    }

    @PostMapping
    public ApiResponse<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        return ApiResponse.ok(messageService.getMessage("ledger.account.created"), accountService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<AccountResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(accountService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<AccountResponse> update(@PathVariable Long id, @Valid @RequestBody CreateAccountRequest request) {
        return ApiResponse.ok(messageService.getMessage("ledger.account.updated"), accountService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return ApiResponse.ok(messageService.getMessage("ledger.account.deleted"), null);
    }
}
