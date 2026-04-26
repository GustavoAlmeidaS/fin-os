package io.github.gustavoalmeidas.finos.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Email String email,
        @Size(max = 80) String firstName,
        @Size(max = 80) String lastName
) {
}
