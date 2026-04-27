package io.github.gustavoalmeidas.finos.importing.domain;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
public class ImportedTransactionRaw {
    private LocalDate transactionDate;
    private BigDecimal normalizedAmount;
    private String rawDescription;
    private String installmentInfo;
    private String externalId;
    private String sourceType;
    private String rawRowPayload;
    private int occurrenceIndex;
    private Map<String, String> metadata;
}
