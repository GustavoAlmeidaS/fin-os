package io.github.gustavoalmeidas.finos.identity.dto;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String username,
        String email
) {
}
