package io.github.gustavoalmeidas.finos.importing.application;

import io.github.gustavoalmeidas.finos.importing.domain.ImportContext;
import io.github.gustavoalmeidas.finos.importing.domain.ImportedTransactionRaw;
import io.github.gustavoalmeidas.finos.importing.domain.StatementDetectionContext;

import java.io.InputStream;
import java.util.List;

public interface StatementParser {
    
    /**
     * Unique identifier for this parser strategy
     */
    String getParserCode();

    /**
     * Checks if this parser can handle the given file context.
     */
    boolean supports(StatementDetectionContext context);

    /**
     * Parses the file content into a list of raw imported transactions.
     */
    List<ImportedTransactionRaw> parse(InputStream input, ImportContext context);
}
