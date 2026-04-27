package io.github.gustavoalmeidas.finos.importing.application;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DescriptionSanitizerTest {

    private final DescriptionSanitizer sanitizer = new DescriptionSanitizer();

    @Test
    void testSanitize_RemovesAccentsAndSpecialChars() {
        String input = "DEBITO VISA ELECTRON BRASIL 13/04 UBER .TRIP HELP.UBE";
        String expected = "debito visa electron brasil uber trip help ube";
        assertEquals(expected, sanitizer.sanitize(input));
    }

    @Test
    void testSanitize_RemovesMaskedCPF() {
        String input = "Transferência enviada pelo Pix - JOAO - ***.854.448-** - BANCO";
        String expected = "transferencia enviada pelo pix joao banco";
        assertEquals(expected, sanitizer.sanitize(input));
    }

    @Test
    void testSanitize_RemovesDates() {
        String input = "Pagamento 2026-04-20";
        String expected = "pagamento";
        assertEquals(expected, sanitizer.sanitize(input));
    }
}
