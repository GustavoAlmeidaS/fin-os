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

class SantanderAccountParserTest {

    private final SantanderAccountParser parser = new SantanderAccountParser();

    @Test
    void testSupports() {
        StatementDetectionContext context = StatementDetectionContext.builder()
                .fileName("ExtratoSantander.xls")
                .build();
        assertTrue(parser.supports(context));
        
        StatementDetectionContext context2 = StatementDetectionContext.builder()
                .firstLine("EXTRATO DE CONTA CORRENTE ,,,,,,")
                .build();
        assertTrue(parser.supports(context2));
    }

    @Test
    void testParseFile() throws Exception {
        Path filePath = Path.of("docs/ExtratoSantander.xls");
        if (!Files.exists(filePath)) {
            System.out.println("Skipping test, file not found: " + filePath);
            return;
        }

        try (InputStream is = new FileInputStream(filePath.toFile())) {
            List<ImportedTransactionRaw> result = parser.parse(is, ImportContext.builder().build());
            assertTrue(result.size() > 0);
            
            org.apache.poi.ss.usermodel.Workbook wb = new org.apache.poi.hssf.usermodel.HSSFWorkbook(new java.io.FileInputStream(filePath.toFile()));
            org.apache.poi.ss.usermodel.Sheet sheet = wb.getSheetAt(0);
            for (int i=0; i<15; i++) {
                org.apache.poi.ss.usermodel.Row r = sheet.getRow(i);
                if(r != null) {
                    System.out.print("ROW " + i + ": ");
                    for (int c=0; c<6; c++) {
                        org.apache.poi.ss.usermodel.Cell cell = r.getCell(c);
                        if(cell != null) {
                            if(cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) System.out.print("[" + cell.getStringCellValue() + "] ");
                            else if(cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) System.out.print("[" + cell.getNumericCellValue() + "] ");
                        }
                    }
                    System.out.println();
                }
            }
            
            boolean foundDebit = false;
            for(ImportedTransactionRaw tx : result) {
                if (tx.getRawDescription().contains("DEBITO VISA")) foundDebit = true;
            }
            assertTrue(foundDebit);
        }
    }
}
