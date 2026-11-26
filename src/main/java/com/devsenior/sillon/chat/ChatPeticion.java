package com.devsenior.sillon.chat;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** El modelo no recuerda nada: el historial completo llega en cada petición. */
public record ChatPeticion(
        @NotBlank(message = "es obligatorio") @Size(max = 100, message = "máximo 100 caracteres") String clienteNombre,
        @NotBlank(message = "es obligatorio") @Size(max = 120, message = "máximo 120 caracteres") String clienteContacto,
        @NotEmpty(message = "debe tener al menos un mensaje") @Size(max = 20, message = "máximo 20 turnos") List<@Valid Turno> historial) {
}
