package com.devsenior.sillon.chat;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record ChatPeticion(
        @NotBlank(message = "El nombre del cliente es obligatorio")
        String clienteNombre,

        @NotBlank(message = "El contacto del cliente es obligatorio")
        @Size(max = 120, message = "El contacto no puede superar 120 caracteres")
        String clienteContacto,

        @NotEmpty(message = "El historial no puede estar vacío")
        @Size(max = 20, message = "El historial no puede superar 20 turnos")
        @Valid
        List<TurnoPeticion> historial) {
}
