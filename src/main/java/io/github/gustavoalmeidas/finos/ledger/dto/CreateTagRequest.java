package io.github.gustavoalmeidas.finos.ledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTagRequest(
        @NotBlank @Size(max = 80) String name,
        @Pattern(regexp = "^#([A-Fa-f0-9]{6})$") String color
) {
}
