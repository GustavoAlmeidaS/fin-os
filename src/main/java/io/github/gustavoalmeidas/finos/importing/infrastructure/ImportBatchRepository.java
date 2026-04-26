package io.github.gustavoalmeidas.finos.importing.infrastructure;

import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.importing.domain.ImportBatch;
import io.github.gustavoalmeidas.finos.importing.domain.ImportBatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {
    Page<ImportBatch> findByUser(User user, Pageable pageable);

    Page<ImportBatch> findByUserAndStatus(User user, ImportBatchStatus status, Pageable pageable);

    Optional<ImportBatch> findByIdAndUser(Long id, User user);
}
