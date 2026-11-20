package com.devsenior.sillon.chat.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "es obligatorio") String clienteNombre,
        @NotBlank(message = "es obligatorio") @Size(max = 120, message = "máximo 120 caracteres") String clienteContacto,
        @NotNull(message = "es obligatorio") @Size(min = 1, max = 20, message = "debe tener entre 1 y 20 turnos")
        List<@NotNull @Valid TurnoChat> historial) {
}
