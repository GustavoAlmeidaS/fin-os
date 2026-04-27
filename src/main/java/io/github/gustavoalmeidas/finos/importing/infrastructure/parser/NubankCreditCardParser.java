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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NubankCreditCardParser implements StatementParser {

    private static final String HEADER_NUBANK_CC = "date,title,amount";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern INSTALLMENT_PATTERN = Pattern.compile("(\\d+\\s*/\\s*\\d+)");

    @Override
    public String getParserCode() {
        return "NUBANK_CREDIT_CARD_CSV";
    }

    @Override
    public boolean supports(StatementDetectionContext context) {
        if (context.getFirstLine() == null) return false;
        return context.getFirstLine().trim().equalsIgnoreCase(HEADER_NUBANK_CC);
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
                
                if (nextLine.length < 3) continue; // skip empty or invalid lines

                String dateStr = nextLine[0].trim();
                String title = nextLine[1].trim();
                String amountStr = nextLine[2].trim();

                if (dateStr.isEmpty() || amountStr.isEmpty()) continue;

                LocalDate date = LocalDate.parse(dateStr, DATE_FORMAT);
                BigDecimal amount = new BigDecimal(amountStr);
                
                // NUBANK RULE: In CC statement, purchases are positive, payments/refunds are negative.
                // We normalize it: expense = negative, income/payment = positive.
                BigDecimal normalizedAmount = amount.negate();

                String installmentInfo = extractInstallment(title);
                
                // Reconstruct raw row for audit
                String rawRow = String.join(",", nextLine);

                transactions.add(ImportedTransactionRaw.builder()
                        .transactionDate(date)
                        .rawDescription(title)
                        .normalizedAmount(normalizedAmount)
                        .installmentInfo(installmentInfo)
                        .sourceType(getParserCode())
                        .rawRowPayload(rawRow)
                        .occurrenceIndex(occurrenceIndex++)
                        .build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing Nubank Credit Card statement: " + e.getMessage(), e);
        }

        return transactions;
    }

    private String extractInstallment(String description) {
        if (description == null) return null;
        Matcher matcher = INSTALLMENT_PATTERN.matcher(description);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
}
