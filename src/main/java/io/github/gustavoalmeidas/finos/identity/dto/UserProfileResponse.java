package io.github.gustavoalmeidas.finos.identity.dto;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean active
) {
}
