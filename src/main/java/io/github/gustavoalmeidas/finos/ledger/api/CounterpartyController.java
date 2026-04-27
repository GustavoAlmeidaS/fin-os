package io.github.gustavoalmeidas.finos.ledger.api;

import io.github.gustavoalmeidas.finos.ledger.application.CounterpartyService;
import io.github.gustavoalmeidas.finos.ledger.dto.CounterpartyResponse;
import io.github.gustavoalmeidas.finos.ledger.dto.CreateCounterpartyRequest;
import io.github.gustavoalmeidas.finos.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/counterparties")
public class CounterpartyController {

    private final CounterpartyService counterpartyService;

    @GetMapping
    public ApiResponse<List<CounterpartyResponse>> list() {
        return ApiResponse.ok(counterpartyService.list());
    }

    @PostMapping
    public ApiResponse<CounterpartyResponse> create(@Valid @RequestBody CreateCounterpartyRequest request) {
        return ApiResponse.ok("Contraparte cadastrada com sucesso.", counterpartyService.create(request));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@org.springframework.web.bind.annotation.PathVariable Long id) {
        counterpartyService.delete(id);
        return ApiResponse.ok("Contraparte removida com sucesso.", null);
    }
}
