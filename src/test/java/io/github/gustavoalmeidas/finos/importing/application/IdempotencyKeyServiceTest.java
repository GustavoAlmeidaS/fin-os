package io.github.gustavoalmeidas.finos.importing.application;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class IdempotencyKeyServiceTest {

    private final IdempotencyKeyService service = new IdempotencyKeyService();

    @Test
    void testGenerateKey_Deterministic() {
        String key1 = service.generateKey(1L, LocalDate.of(2026, 4, 24), new BigDecimal("-14.00"), "uber", null, 0, null);
        String key2 = service.generateKey(1L, LocalDate.of(2026, 4, 24), new BigDecimal("-14.00"), "uber", null, 0, null);
        
        assertEquals(key1, key2);
    }

    @Test
    void testGenerateKey_DifferentOccurrence_DifferentKeys() {
        String key1 = service.generateKey(1L, LocalDate.of(2026, 4, 24), new BigDecimal("-14.00"), "uber", null, 0, null);
        String key2 = service.generateKey(1L, LocalDate.of(2026, 4, 24), new BigDecimal("-14.00"), "uber", null, 1, null);
        
        assertNotEquals(key1, key2);
    }

    @Test
    void testGenerateKey_WithExternalId() {
        String key1 = service.generateKey(1L, LocalDate.of(2026, 4, 24), new BigDecimal("-14.00"), "uber", null, 0, "uuid-1234");
        String key2 = service.generateKey(1L, LocalDate.of(2026, 4, 24), new BigDecimal("-14.00"), "uber", null, 0, "uuid-1234");
        
        assertEquals(key1, key2);
    }
}
