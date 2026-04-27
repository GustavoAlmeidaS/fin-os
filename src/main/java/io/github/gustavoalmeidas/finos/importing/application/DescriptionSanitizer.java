package io.github.gustavoalmeidas.finos.importing.application;

import org.springframework.stereotype.Service;
import java.text.Normalizer;

@Service
public class DescriptionSanitizer {

    public String sanitize(String rawDescription) {
        if (rawDescription == null || rawDescription.isBlank()) {
            return "";
        }

        // 1. Lowercase
        String sanitized = rawDescription.toLowerCase();

        // 2. Remove accents
        sanitized = Normalizer.normalize(sanitized, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // 3. Remove CPFs/CNPJs masked (e.g. ***.123.456-**)
        sanitized = sanitized.replaceAll("\\*+\\.\\d{3}\\.\\d{3}-\\*+", "");
        sanitized = sanitized.replaceAll("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}", ""); // exact cpf

        // 4. Remove dates (e.g. 13/04, 2026-04-20)
        sanitized = sanitized.replaceAll("\\d{2}/\\d{2}(/\\d{4})?", "");
        sanitized = sanitized.replaceAll("\\d{4}-\\d{2}-\\d{2}", "");

        // 5. Keep only alphanumeric and spaces
        sanitized = sanitized.replaceAll("[^a-z0-9\\s]", " ");

        // 6. Remove isolated numbers or short noise (length 1 or 2, unless meaningful)
        // Here we just remove purely numeric blocks
        sanitized = sanitized.replaceAll("\\b\\d+\\b", " ");

        // 7. Collapse multiple spaces and trim
        sanitized = sanitized.replaceAll("\\s+", " ").trim();

        return sanitized;
    }
}
