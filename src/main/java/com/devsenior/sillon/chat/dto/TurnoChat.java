package com.devsenior.sillon.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TurnoChat(
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "CLIENTE|ASISTENTE", message = "debe ser CLIENTE o ASISTENTE") String rol,
        @NotBlank(message = "es obligatorio") @Size(max = 500, message = "máximo 500 caracteres") String texto) {
}
