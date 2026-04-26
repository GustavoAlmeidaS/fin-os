package io.github.gustavoalmeidas.finos.importing.domain;

public enum ImportBatchStatus {
    RECEIVED,
    PREVIEWED,
    PROCESSING,
    IMPORTED,
    PARTIALLY_IMPORTED,
    FAILED,
    CANCELLED
}
