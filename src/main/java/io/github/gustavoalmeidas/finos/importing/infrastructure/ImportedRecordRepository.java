package io.github.gustavoalmeidas.finos.importing.infrastructure;

import io.github.gustavoalmeidas.finos.importing.domain.ImportBatch;
import io.github.gustavoalmeidas.finos.importing.domain.ImportedRecord;
import io.github.gustavoalmeidas.finos.importing.domain.ImportedRecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportedRecordRepository extends JpaRepository<ImportedRecord, Long> {
    Page<ImportedRecord> findByBatch(ImportBatch batch, Pageable pageable);

    List<ImportedRecord> findByBatchAndStatus(ImportBatch batch, ImportedRecordStatus status);

    boolean existsByDeduplicationHashAndStatus(String hash, ImportedRecordStatus status);
}
