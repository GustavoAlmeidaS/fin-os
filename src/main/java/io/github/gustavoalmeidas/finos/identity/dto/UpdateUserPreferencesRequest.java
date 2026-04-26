package io.github.gustavoalmeidas.finos.identity.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserPreferencesRequest(
        @Size(min = 2, max = 10) String defaultLocale,
        @Size(min = 3, max = 80) String defaultTimezone,
        @Pattern(regexp = "^[A-Z]{3}$") String defaultCurrency,
        @Size(min = 6, max = 20) String dateFormat,
        @Size(min = 2, max = 10) String numberFormatLocale
) {
}
