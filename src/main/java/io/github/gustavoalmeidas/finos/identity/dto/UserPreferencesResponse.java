package io.github.gustavoalmeidas.finos.identity.dto;

public record UserPreferencesResponse(
        String defaultLocale,
        String defaultTimezone,
        String defaultCurrency,
        String dateFormat,
        String numberFormatLocale
) {
}
