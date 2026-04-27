package io.github.gustavoalmeidas.finos.importing.api;

import io.github.gustavoalmeidas.finos.identity.application.UserService;
import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.importing.application.ImportService;
import io.github.gustavoalmeidas.finos.importing.application.TransactionImportService;
import io.github.gustavoalmeidas.finos.importing.domain.ImportBatch;
import io.github.gustavoalmeidas.finos.importing.domain.ImportBatchStatus;
import io.github.gustavoalmeidas.finos.importing.dto.ImportBatchResponse;
import io.github.gustavoalmeidas.finos.importing.mapper.ImportMapper;
import io.github.gustavoalmeidas.finos.ledger.application.AccountService;
import io.github.gustavoalmeidas.finos.ledger.domain.Account;
import io.github.gustavoalmeidas.finos.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/imports")
public class ImportController {

    private final TransactionImportService transactionImportService;
    private final ImportService legacyImportService; // keeping for list/get methods if needed
    private final UserService userService;
    private final AccountService accountService;
    private final ImportMapper mapper;

    @PostMapping("/csv")
    public ApiResponse<ImportBatchResponse> uploadCsv(
            @RequestParam Long accountId,
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        User user = userService.currentUser();
        Account account = accountService.getOwnedEntity(accountId);
        
        ImportBatch batch = transactionImportService.processImport(file.getInputStream(), file.getOriginalFilename(), account, user);
        
        return ApiResponse.ok("Importação concluída.", mapper.toBatchResponse(batch));
    }

    @GetMapping
    public ApiResponse<List<ImportBatchResponse>> list(
            @RequestParam(required = false) ImportBatchStatus status,
            Pageable pageable
    ) {
        Page<ImportBatchResponse> page = legacyImportService.list(status, pageable);
        return ApiResponse.ok(null, page.getContent(), pageMeta(page));
    }

    @GetMapping("/{batchId}")
    public ApiResponse<ImportBatchResponse> getBatch(@PathVariable Long batchId) {
        return ApiResponse.ok(legacyImportService.getBatch(batchId));
    }

    // Retaining confirm/cancel as no-ops or calling legacy just in case UI requires them
    @PostMapping("/{batchId}/confirm")
    public ApiResponse<ImportBatchResponse> confirm(@PathVariable Long batchId) {
        return ApiResponse.ok("Importação confirmada com sucesso.", legacyImportService.confirmImport(batchId));
    }

    @PostMapping("/{batchId}/cancel")
    public ApiResponse<ImportBatchResponse> cancel(@PathVariable Long batchId) {
        return ApiResponse.ok("Lote de importação cancelado.", legacyImportService.cancelBatch(batchId));
    }

    private Map<String, Object> pageMeta(Page<?> page) {
        return Map.of(
                "page", page.getNumber(),
                "size", page.getSize(),
                "totalElements", page.getTotalElements(),
                "totalPages", page.getTotalPages()
        );
    }
}
