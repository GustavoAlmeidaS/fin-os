package io.github.gustavoalmeidas.finos.importing.dto;

import io.github.gustavoalmeidas.finos.importing.domain.ImportedRecordStatus;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ImportedRecordResponse(
        Long id,
        int lineNumber,
        String rawPayload,
        LocalDate parsedDate,
        BigDecimal parsedAmount,
        String parsedDescription,
        TransactionType parsedType,
        String deduplicationHash,
        ImportedRecordStatus status,
        String errorMessage,
        Long transactionId
) {
}
