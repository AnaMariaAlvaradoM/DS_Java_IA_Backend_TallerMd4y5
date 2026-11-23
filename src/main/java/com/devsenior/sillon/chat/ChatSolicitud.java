package com.devsenior.sillon.chat;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatSolicitud(

        @NotBlank(message = "El nombre del cliente es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String clienteNombre,

        @NotBlank(message = "El contacto del cliente es obligatorio")
        @Size(max = 120, message = "El contacto no puede superar 120 caracteres")
        String clienteContacto,

        @NotEmpty(message = "La conversación no puede estar vacía")
        @Size(max = 20, message = "La conversación no puede superar 20 turnos")
        List<@Valid Turno> historial
) {

    public enum Rol {
        CLIENTE,
        ASISTENTE
    }

    public record Turno(

            @NotNull(message = "El rol es obligatorio")
            Rol rol,

            @NotBlank(message = "El texto es obligatorio")
            @Size(max = 500, message = "Cada turno puede tener máximo 500 caracteres")
            String texto
    ) {
    }
}
