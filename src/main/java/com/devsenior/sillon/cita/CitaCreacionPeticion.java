package com.devsenior.sillon.cita;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Peticion para crear una cita desde el salon o desde la reserva en linea.
 */
public record CitaCreacionPeticion(
        @NotBlank(message = "El nombre del cliente es obligatorio")
        String clienteNombre,

        @NotBlank(message = "El teléfono del cliente es obligatorio")
        @Size(min = 7, max = 30, message = "El teléfono del cliente debe tener entre 7 y 30 caracteres")
        String clienteTelefono,

        @NotNull(message = "El servicio es obligatorio")
        Long servicioId,

        @NotNull(message = "El estilista es obligatorio")
        Long estilistaId,

        @NotNull(message = "La fecha y hora de inicio son obligatorias")
        LocalDateTime inicio,

        @Size(max = 300, message = "La nota no puede superar los 300 caracteres")
        String nota) {
}
