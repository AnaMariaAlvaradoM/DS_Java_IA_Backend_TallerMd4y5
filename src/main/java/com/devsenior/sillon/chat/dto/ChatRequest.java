package com.devsenior.sillon.chat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** El modelo no recuerda: el historial completo llega en cada petición (de 1 a 20 turnos). */
public record ChatRequest(
        @NotBlank(message = "es obligatorio") @Size(max = 100, message = "máximo 100 caracteres") String clienteNombre,
        @NotBlank(message = "es obligatorio") @Size(max = 120, message = "máximo 120 caracteres") String clienteContacto,
        @NotNull(message = "es obligatorio")
        @Size(min = 1, max = 20, message = "debe tener entre 1 y 20 turnos") List<@Valid @NotNull TurnoChat> historial
) {
}
