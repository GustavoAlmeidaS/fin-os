package io.github.gustavoalmeidas.finos.importing.infrastructure.parser;

import com.opencsv.CSVReader;
import io.github.gustavoalmeidas.finos.importing.application.StatementParser;
import io.github.gustavoalmeidas.finos.importing.domain.ImportContext;
import io.github.gustavoalmeidas.finos.importing.domain.ImportedTransactionRaw;
import io.github.gustavoalmeidas.finos.importing.domain.StatementDetectionContext;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class NubankAccountParser implements StatementParser {

    private static final String HEADER_NUBANK_ACCOUNT = "Data,Valor,Identificador,Descrição";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public String getParserCode() {
        return "NUBANK_CHECKING_ACCOUNT_CSV";
    }

    @Override
    public boolean supports(StatementDetectionContext context) {
        if (context.getFirstLine() == null) return false;
        return context.getFirstLine().trim().equalsIgnoreCase(HEADER_NUBANK_ACCOUNT);
    }

    @Override
    public List<ImportedTransactionRaw> parse(InputStream input, ImportContext context) {
        List<ImportedTransactionRaw> transactions = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String[] nextLine;
            boolean firstLine = true;
            int occurrenceIndex = 0;

            while ((nextLine = reader.readNext()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue; // skip header
                }
                
                if (nextLine.length < 4) continue; // skip invalid lines

                String dateStr = nextLine[0].trim();
                String amountStr = nextLine[1].trim();
                String identificador = nextLine[2].trim();
                String descricao = nextLine[3].trim();

                if (dateStr.isEmpty() || amountStr.isEmpty()) continue;

                LocalDate date = LocalDate.parse(dateStr, DATE_FORMAT);
                BigDecimal amount = new BigDecimal(amountStr); // already has correct semantic sign
                
                String rawRow = String.join(",", nextLine);

                transactions.add(ImportedTransactionRaw.builder()
                        .transactionDate(date)
                        .rawDescription(descricao)
                        .normalizedAmount(amount)
                        .externalId(identificador.isEmpty() ? null : identificador)
                        .sourceType(getParserCode())
                        .rawRowPayload(rawRow)
                        .occurrenceIndex(occurrenceIndex++)
                        .build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing Nubank Account statement: " + e.getMessage(), e);
        }

        return transactions;
    }
}
