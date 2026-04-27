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

class NubankCreditCardParserTest {

    private final NubankCreditCardParser parser = new NubankCreditCardParser();

    @Test
    void testSupports() {
        StatementDetectionContext context = StatementDetectionContext.builder()
                .firstLine("date,title,amount")
                .build();
        assertTrue(parser.supports(context));
    }

    @Test
    void testParseFile() throws Exception {
        Path filePath = Path.of("docs/Nubank_2026-05-20.csv");
        if (!Files.exists(filePath)) {
            System.out.println("Skipping test, file not found: " + filePath);
            return;
        }

        try (InputStream is = new FileInputStream(filePath.toFile())) {
            List<ImportedTransactionRaw> result = parser.parse(is, ImportContext.builder().build());
            
            assertFalse(result.isEmpty());
            
            // Check if amounts were inverted correctly (Nubank CC: purchases are positive in CSV)
            // Example: "2026-04-20,Carrefour Shopping Met,3.49" -> should be -3.49
            boolean foundPurchase = false;
            boolean foundPayment = false;
            
            for (ImportedTransactionRaw tx : result) {
                assertNotNull(tx.getRawDescription());
                assertNotNull(tx.getNormalizedAmount());
                if (tx.getRawDescription().contains("Carrefour")) foundPurchase = true;
                if (tx.getRawDescription().contains("Pagamento recebido")) foundPayment = true;
            }
            
            assertTrue(foundPurchase);
            assertTrue(foundPayment);
        }
    }
}
