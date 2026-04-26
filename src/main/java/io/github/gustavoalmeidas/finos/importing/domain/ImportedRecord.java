package io.github.gustavoalmeidas.finos.importing.domain;

import io.github.gustavoalmeidas.finos.ledger.domain.Transaction;
import io.github.gustavoalmeidas.finos.ledger.domain.TransactionType;
import io.github.gustavoalmeidas.finos.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "imported_records")
public class ImportedRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private ImportBatch batch;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "parsed_date")
    private LocalDate parsedDate;

    @Column(name = "parsed_amount", precision = 14, scale = 2)
    private BigDecimal parsedAmount;

    @Column(name = "parsed_description")
    private String parsedDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "parsed_type", length = 30)
    private TransactionType parsedType;

    @Column(name = "deduplication_hash", nullable = false, length = 64)
    private String deduplicationHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ImportedRecordStatus status = ImportedRecordStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;
}
