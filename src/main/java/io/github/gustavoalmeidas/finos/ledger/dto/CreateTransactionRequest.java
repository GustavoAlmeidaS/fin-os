package io.github.gustavoalmeidas.finos.ledger.dto;

import io.github.gustavoalmeidas.finos.ledger.domain.TransactionSource;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionStatus;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionType;
import io.github.gustavoalmeidas.finos.ledger.domain.RecurrenceFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record CreateTransactionRequest(
        @NotNull Long accountId,
        Long destinationAccountId,
        @NotNull TransactionType type,
        TransactionStatus status,
        TransactionSource source,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull LocalDate transactionDate,
        @NotBlank @Size(max = 255) String description,
        String notes,
        Long categoryId,
        Long counterpartyId,
        Set<Long> tagIds,
        List<TransactionSplitRequest> splits,
        Long importBatchId,
        RecurrenceFrequency recurrenceFrequency,
        LocalDate recurrenceEndDate,
        String metadata
) {
}
