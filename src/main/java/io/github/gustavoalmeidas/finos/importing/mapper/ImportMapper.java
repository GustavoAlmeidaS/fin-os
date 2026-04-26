package io.github.gustavoalmeidas.finos.importing.mapper;

import io.github.gustavoalmeidas.finos.importing.domain.ImportBatch;
import io.github.gustavoalmeidas.finos.importing.domain.ImportedRecord;
import io.github.gustavoalmeidas.finos.importing.dto.ImportBatchResponse;
import io.github.gustavoalmeidas.finos.importing.dto.ImportedRecordResponse;
import org.springframework.stereotype.Component;

@Component
public class ImportMapper {

    public ImportBatchResponse toBatchResponse(ImportBatch batch) {
        return new ImportBatchResponse(
                batch.getId(),
                batch.getAccount().getId(),
                batch.getAccount().getName(),
                batch.getFilename(),
                batch.getFileSize(),
                batch.getStatus(),
                batch.getTotalRecords(),
                batch.getValidRecords(),
                batch.getInvalidRecords(),
                batch.getDuplicateRecords(),
                batch.getErrorMessage(),
                batch.getProcessedAt()
        );
    }

    public ImportedRecordResponse toRecordResponse(ImportedRecord record) {
        return new ImportedRecordResponse(
                record.getId(),
                record.getLineNumber(),
                record.getRawPayload(),
                record.getParsedDate(),
                record.getParsedAmount(),
                record.getParsedDescription(),
                record.getParsedType(),
                record.getDeduplicationHash(),
                record.getStatus(),
                record.getErrorMessage(),
                record.getTransaction() == null ? null : record.getTransaction().getId()
        );
    }
}
