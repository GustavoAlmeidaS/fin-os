package io.github.gustavoalmeidas.finos.importing.api;

import io.github.gustavoalmeidas.finos.importing.application.ImportService;
import io.github.gustavoalmeidas.finos.importing.domain.ImportBatchStatus;
import io.github.gustavoalmeidas.finos.importing.dto.ImportBatchResponse;
import io.github.gustavoalmeidas.finos.importing.dto.ImportedRecordResponse;
import io.github.gustavoalmeidas.finos.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/imports")
public class ImportController {

    private final ImportService importService;

    @PostMapping("/csv")
    public ApiResponse<ImportBatchResponse> uploadCsv(
            @RequestParam Long accountId,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.ok("Arquivo recebido para pré-visualização.", importService.createBatchFromCsv(accountId, file));
    }

    @GetMapping
    public ApiResponse<List<ImportBatchResponse>> list(
            @RequestParam(required = false) ImportBatchStatus status,
            Pageable pageable
    ) {
        Page<ImportBatchResponse> page = importService.list(status, pageable);
        return ApiResponse.ok(null, page.getContent(), pageMeta(page));
    }

    @GetMapping("/{batchId}")
    public ApiResponse<ImportBatchResponse> getBatch(@PathVariable Long batchId) {
        return ApiResponse.ok(importService.getBatch(batchId));
    }

    @GetMapping("/{batchId}/records")
    public ApiResponse<List<ImportedRecordResponse>> preview(@PathVariable Long batchId, Pageable pageable) {
        Page<ImportedRecordResponse> page = importService.previewBatch(batchId, pageable);
        return ApiResponse.ok(null, page.getContent(), pageMeta(page));
    }

    @PostMapping("/{batchId}/confirm")
    public ApiResponse<ImportBatchResponse> confirm(@PathVariable Long batchId) {
        return ApiResponse.ok("Importação confirmada com sucesso.", importService.confirmImport(batchId));
    }

    @PostMapping("/{batchId}/cancel")
    public ApiResponse<ImportBatchResponse> cancel(@PathVariable Long batchId) {
        return ApiResponse.ok("Lote de importação cancelado.", importService.cancelBatch(batchId));
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
