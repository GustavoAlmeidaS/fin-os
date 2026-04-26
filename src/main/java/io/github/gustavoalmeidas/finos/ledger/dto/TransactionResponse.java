package io.github.gustavoalmeidas.finos.ledger.dto;

import io.github.gustavoalmeidas.finos.ledger.domain.TransactionSource;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionStatus;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionType;
import io.github.gustavoalmeidas.finos.ledger.domain.RecurrenceFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record TransactionResponse(
        Long id,
        Long accountId,
        String accountName,
        Long destinationAccountId,
        String destinationAccountName,
        TransactionType type,
        TransactionStatus status,
        TransactionSource source,
        BigDecimal amount,
        LocalDate transactionDate,
        String description,
        String notes,
        Long categoryId,
        String categoryName,
        CounterpartyResponse counterparty,
        Set<TagResponse> tags,
        List<TransactionSplitResponse> splits,
        Long importBatchId,
        String metadata,
        RecurrenceFrequency recurrenceFrequency,
        LocalDate recurrenceEndDate
) {
}
