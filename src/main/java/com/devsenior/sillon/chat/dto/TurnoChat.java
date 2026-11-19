package com.devsenior.sillon.chat.dto;

import com.devsenior.sillon.chat.model.RolTurno;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TurnoChat(
        @NotNull(message = "es obligatorio (CLIENTE o ASISTENTE)") RolTurno rol,
        @NotBlank(message = "es obligatorio") @Size(max = 500, message = "máximo 500 caracteres") String texto
) {
}
