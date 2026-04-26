package io.github.gustavoalmeidas.finos.ledger.api;

import io.github.gustavoalmeidas.finos.ledger.application.TransactionService;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionType;
import io.github.gustavoalmeidas.finos.ledger.dto.CreateTransactionRequest;
import io.github.gustavoalmeidas.finos.ledger.dto.TransactionResponse;
import io.github.gustavoalmeidas.finos.shared.api.ApiResponse;
import io.github.gustavoalmeidas.finos.shared.i18n.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final MessageService messageService;

    @GetMapping
    public ApiResponse<java.util.List<TransactionResponse>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Long counterpartyId,
            @RequestParam(required = false) String tag,
            Pageable pageable
    ) {
        Page<TransactionResponse> page = transactionService.list(startDate, endDate, accountId, type, counterpartyId, tag, pageable);
        return ApiResponse.ok(null, page.getContent(), Map.of(
                "page", page.getNumber(),
                "size", page.getSize(),
                "totalElements", page.getTotalElements(),
                "totalPages", page.getTotalPages()
        ));
    }

    @PostMapping
    public ApiResponse<TransactionResponse> create(@Valid @RequestBody CreateTransactionRequest request) {
        return ApiResponse.ok(messageService.getMessage("ledger.transaction.created"), transactionService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<TransactionResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(transactionService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<TransactionResponse> update(@PathVariable Long id, @Valid @RequestBody CreateTransactionRequest request) {
        return ApiResponse.ok(messageService.getMessage("ledger.transaction.updated"), transactionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ApiResponse.ok(messageService.getMessage("ledger.transaction.deleted"), null);
    }
}
