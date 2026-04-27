package io.github.gustavoalmeidas.finos.importing.infrastructure.parser;

import io.github.gustavoalmeidas.finos.importing.domain.ImportContext;
import io.github.gustavoalmeidas.finos.importing.domain.ImportedTransactionRaw;
import io.github.gustavoalmeidas.finos.importing.domain.StatementDetectionContext;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NubankAccountParserTest {

    private final NubankAccountParser parser = new NubankAccountParser();

    @Test
    void testSupports() {
        StatementDetectionContext context = StatementDetectionContext.builder()
                .firstLine("Data,Valor,Identificador,Descrição")
                .build();
        assertTrue(parser.supports(context));
    }

    @Test
    void testParseFile() throws Exception {
        Path filePath = Path.of("docs/NU_535477899_01ABR2026_25ABR2026.csv");
        if (!Files.exists(filePath)) {
            System.out.println("Skipping test, file not found: " + filePath);
            return;
        }

        try (InputStream is = new FileInputStream(filePath.toFile())) {
            List<ImportedTransactionRaw> result = parser.parse(is, ImportContext.builder().build());
            
            assertFalse(result.isEmpty());
            
            boolean foundCredit = false;
            boolean foundDebit = false;
            
            for (ImportedTransactionRaw tx : result) {
                assertNotNull(tx.getNormalizedAmount());
                if (tx.getRawDescription().contains("Crédito em conta")) {
                    assertNotNull(tx.getExternalId());
                    foundCredit = true;
                }
                if (tx.getRawDescription().contains("Compra no débito")) {
                    assertTrue(tx.getNormalizedAmount().compareTo(BigDecimal.ZERO) < 0);
                    foundDebit = true;
                }
            }
            
            assertTrue(foundCredit);
            assertTrue(foundDebit);
        }
    }
}
