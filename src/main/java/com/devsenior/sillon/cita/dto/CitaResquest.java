package com.devsenior.sillon.cita.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

//clienteNombre, clienteTelefono (7 a 30
//caracteres), servicioId, estilistaId, inicio,
//nota (opcional, máx. 300)
public record CitaResquest(
        @NotBlank(message = "El nombre del cliente es obligatorio")
        String clienteNombre,

        @NotBlank(message = "El teléfono es obligatorio")
        @Size(min = 7, max = 30, message = "El teléfono debe tener entre 7 y 30 caracteres")
        String clienteTelefono,

        @NotNull(message = "El ID del servicio es obligatorio")
        Long servicioId,

        @NotNull(message = "El ID del estilista es obligatorio")
        Long estilistaId,

        @NotNull(message = "La fecha y hora de inicio es obligatoria")
        LocalDateTime inicio,

        @Size(max = 300, message = "La nota no puede superar los 300 caracteres")
        String nota
) {
}
