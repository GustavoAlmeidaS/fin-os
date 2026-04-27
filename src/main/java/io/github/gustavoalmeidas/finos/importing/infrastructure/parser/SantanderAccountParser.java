package io.github.gustavoalmeidas.finos.importing.infrastructure.parser;

import com.opencsv.CSVReader;
import io.github.gustavoalmeidas.finos.importing.application.StatementParser;
import io.github.gustavoalmeidas.finos.importing.domain.ImportContext;
import io.github.gustavoalmeidas.finos.importing.domain.ImportedTransactionRaw;
import io.github.gustavoalmeidas.finos.importing.domain.StatementDetectionContext;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class SantanderAccountParser implements StatementParser {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public String getParserCode() {
        return "SANTANDER_ACCOUNT_MIXED";
    }

    @Override
    public boolean supports(StatementDetectionContext context) {
        if (context.getFileName() != null && context.getFileName().toLowerCase().contains("santander")) {
            return true;
        }
        if (context.getFirstLine() != null && context.getFirstLine().contains("EXTRATO DE CONTA CORRENTE")) {
            return true;
        }
        return false;
    }

    @Override
    public List<ImportedTransactionRaw> parse(InputStream input, ImportContext context) {
        try {
            byte[] fileBytes = input.readAllBytes();
            
            // Check if it's an OLE2 format (legacy XLS) by checking magic bytes
            if (isOle2Format(fileBytes)) {
                return parseXls(new ByteArrayInputStream(fileBytes));
            } else {
                // Fallback to CSV
                return parseCsv(new ByteArrayInputStream(fileBytes));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing Santander statement: " + e.getMessage(), e);
        }
    }

    private boolean isOle2Format(byte[] bytes) {
        if (bytes == null || bytes.length < 8) return false;
        return bytes[0] == (byte) 0xD0 && bytes[1] == (byte) 0xCF &&
               bytes[2] == (byte) 0x11 && bytes[3] == (byte) 0xE0 &&
               bytes[4] == (byte) 0xA1 && bytes[5] == (byte) 0xB1 &&
               bytes[6] == (byte) 0x1A && bytes[7] == (byte) 0xE1;
    }

    private List<ImportedTransactionRaw> parseXls(InputStream input) throws Exception {
        List<ImportedTransactionRaw> transactions = new ArrayList<>();
        try (Workbook workbook = new HSSFWorkbook(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            int occurrenceIndex = 0;

            for (Row row : sheet) {
                if (isFooterRow(getCellValue(row.getCell(0)) + " " + getCellValue(row.getCell(1)))) {
                    break;
                }

                String dateStr = getCellValue(row.getCell(0));
                String descStr = getCellValue(row.getCell(1));
                String creditStr = getCellValue(row.getCell(4));
                String debitStr = getCellValue(row.getCell(5));

                ImportedTransactionRaw tx = processRow(dateStr, descStr, creditStr, debitStr, occurrenceIndex);
                if (tx != null) {
                    tx.setRawRowPayload("XLS Row " + row.getRowNum());
                    transactions.add(tx);
                    occurrenceIndex++;
                }
            }
        }
        return transactions;
    }

    private List<ImportedTransactionRaw> parseCsv(InputStream input) throws Exception {
        List<ImportedTransactionRaw> transactions = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String[] nextLine;
            int occurrenceIndex = 0;

            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length < 6) continue;

                String fullRowStr = String.join(",", nextLine);
                if (isFooterRow(fullRowStr)) {
                    break; // stop processing
                }

                String dateStr = nextLine[0].trim();
                String descStr = nextLine[1].trim();
                String creditStr = nextLine[4].trim();
                String debitStr = nextLine[5].trim();

                ImportedTransactionRaw tx = processRow(dateStr, descStr, creditStr, debitStr, occurrenceIndex);
                if (tx != null) {
                    tx.setRawRowPayload(fullRowStr);
                    transactions.add(tx);
                    occurrenceIndex++;
                }
            }
        }
        return transactions;
    }

    private ImportedTransactionRaw processRow(String dateStr, String descStr, String creditStr, String debitStr, int occurrenceIndex) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr.trim(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null; // Not a transaction row (e.g., header or empty)
        }

        if (descStr != null && descStr.trim().equalsIgnoreCase("SALDO ANTERIOR")) {
            return null;
        }

        BigDecimal credit = parseBrMonetaryValue(creditStr);
        BigDecimal debit = parseBrMonetaryValue(debitStr);

        if (credit == null && debit == null) {
            return null; // No values
        }

        BigDecimal amount = BigDecimal.ZERO;
        if (credit != null && credit.compareTo(BigDecimal.ZERO) != 0) {
            amount = credit.abs(); // Ensure positive
        } else if (debit != null && debit.compareTo(BigDecimal.ZERO) != 0) {
            amount = debit.abs().negate(); // Ensure negative
        }

        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return ImportedTransactionRaw.builder()
                .transactionDate(date)
                .rawDescription(descStr == null ? "" : descStr.trim())
                .normalizedAmount(amount)
                .sourceType(getParserCode())
                .occurrenceIndex(occurrenceIndex)
                .build();
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return "";
        }
    }

    private boolean isFooterRow(String lineText) {
        if (lineText == null) return false;
        String upper = lineText.toUpperCase();
        return upper.contains("TOTAL") || 
               upper.contains("SALDO DE CONTA") ||
               upper.contains("SALDO DISPONÍVEL") ||
               upper.contains("SALDO DISPONIVEL") ||
               upper.contains("JUROS CALCULADOS") ||
               upper.contains("IOF ACUMULADOS");
    }

    private BigDecimal parseBrMonetaryValue(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        String cleaned = val.replaceAll("\"", "").trim();
        if (cleaned.isEmpty()) return null;
        
        // Remove points (thousands) and replace comma with point (decimals)
        cleaned = cleaned.replace(".", "");
        cleaned = cleaned.replace(",", ".");
        
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
