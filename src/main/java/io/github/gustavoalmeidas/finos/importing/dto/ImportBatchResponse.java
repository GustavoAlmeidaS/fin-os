package io.github.gustavoalmeidas.finos.importing.dto;

import io.github.gustavoalmeidas.finos.importing.domain.ImportBatchStatus;

import java.time.LocalDateTime;

public record ImportBatchResponse(
        Long id,
        Long accountId,
        String accountName,
        String filename,
        Long fileSize,
        ImportBatchStatus status,
        int totalRecords,
        int validRecords,
        int invalidRecords,
        int duplicateRecords,
        String errorMessage,
        LocalDateTime processedAt
) {
}
