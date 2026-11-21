package com.devsenior.sillon.chat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChatRequest(
        @NotBlank(message = "El nombre del cliente es obligatorio")
        @Size(max = 100, message = "El nombre del cliente no debe superar 100 caracteres")
        String clienteNombre,

        @NotBlank(message = "El contacto del cliente es obligatorio")
        @Size(max = 120, message = "El contacto del cliente no debe superar 120 caracteres")
        String clienteContacto,

        @NotNull(message = "El historial es obligatorio")
        @Size(min = 1, max = 20, message = "El historial debe tener entre 1 y 20 turnos")
        @Valid
        List<TurnoChatDTO> historial
) {
}
