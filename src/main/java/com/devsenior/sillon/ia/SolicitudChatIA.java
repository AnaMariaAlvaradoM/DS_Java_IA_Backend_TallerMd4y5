
package com.devsenior.sillon.ia;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudChatIA(

        @NotBlank(message = "El nombre del cliente es obligatorio")
        String clienteNombre,

        @NotBlank(message = "El contacto del cliente es obligatorio")
        @Size(
                max = 120,
                message = "El contacto no puede superar 120 caracteres"
        )
        String clienteContacto,

        @NotNull(message = "El historial es obligatorio")
        @Size(
                min = 1,
                max = 20,
                message = "El historial debe contener entre 1 y 20 mensajes"
        )
        List<@NotNull @Valid Turno> historial

) {

    public record Turno(

            @NotNull(message = "El rol es obligatorio")
            RolTurno rol,

            @NotBlank(message = "El texto es obligatorio")
            @Size(
                    max = 500,
                    message = "Cada mensaje puede tener máximo 500 caracteres"
            )
            String texto

    ) {
    }

    public enum RolTurno {
        CLIENTE,
        ASISTENTE
    }
}
