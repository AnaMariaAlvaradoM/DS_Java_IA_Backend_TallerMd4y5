package com.devsenior.sillon.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TurnoPeticion(
        @NotNull(message = "El rol del turno es obligatorio")
        RolTurno rol,

        @NotBlank(message = "El texto del turno no puede estar vacío")
        @Size(max = 500, message = "El texto del turno no puede superar 500 caracteres")
        String texto) {
}
