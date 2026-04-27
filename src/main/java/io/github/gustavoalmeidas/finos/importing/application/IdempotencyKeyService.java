package io.github.gustavoalmeidas.finos.importing.application;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class IdempotencyKeyService {

    /**
     * Generates a deterministic SHA-256 hash for deduplication.
     */
    public String generateKey(Long accountId, LocalDate date, BigDecimal amount, String normalizedDesc, String installment, int occurrenceIndex, String externalId) {
        
        // If an exact external ID is provided (like Nubank Account UUID), we can use it, but to prevent cross-account clashes or split transaction issues,
        // we combine it with the account ID.
        if (externalId != null && !externalId.isBlank()) {
            return hash(accountId + "|" + externalId);
        }

        // Fallback synthetic hash for Santander or Nubank CC
        String base = String.format("%d|%s|%s|%s|%s|%d",
                accountId,
                date.toString(),
                amount.toPlainString(),
                normalizedDesc,
                installment != null ? installment : "",
                occurrenceIndex);

        return hash(base);
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
