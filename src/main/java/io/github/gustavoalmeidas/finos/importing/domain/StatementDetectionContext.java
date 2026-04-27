package io.github.gustavoalmeidas.finos.importing.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatementDetectionContext {
    private String fileName;
    private String contentType;
    private String firstLine;
    private byte[] fileContent; // or InputStream, but byte[] is easier to read repeatedly for detection
}
