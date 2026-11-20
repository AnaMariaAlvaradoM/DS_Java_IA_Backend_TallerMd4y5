package com.devsenior.sillon.ia.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChatRequest(
        @NotBlank(message = "El nombre del cliente es obligatorio") @Size(max = 100, message = "El nombre no puede superar 100 caracteres") String clienteNombre,

        @NotBlank(message = "El contacto del cliente es obligatorio") @Size(max = 120, message = "El contacto no puede superar 120 caracteres") String clienteContacto,

        @NotEmpty(message = "El historial debe tener al menos un mensaje") @Size(min = 1, max = 20, message = "El historial debe tener entre 1 y 20 turnos") @Valid List<TurnoChat> historial) {
    public enum RolChat {
        CLIENTE,
        ASISTENTE
    }

    public record TurnoChat(
            @NotNull(message = "El rol del turno es obligatorio") RolChat rol,

            @NotBlank(message = "El texto del turno es obligatorio") @Size(max = 500, message = "El mensaje no puede superar 500 caracteres") String texto) {
    }
}