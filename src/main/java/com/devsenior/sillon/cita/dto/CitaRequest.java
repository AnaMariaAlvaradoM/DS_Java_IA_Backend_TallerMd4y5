package com.devsenior.sillon.cita.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CitaRequest(
        @NotBlank(message = "El nombre del cliente es obligatorio") @Size(max = 100, message = "El nombre del cliente no puede superar 100 caracteres") String clienteNombre,

        @NotBlank(message = "El teléfono del cliente es obligatorio") @Size(min = 7, max = 30, message = "El teléfono debe tener entre 7 y 30 caracteres") String clienteTelefono,

        @NotNull(message = "El servicio es obligatorio") Long servicioId,

        @NotNull(message = "El estilista es obligatorio") Long estilistaId,

        @NotNull(message = "La fecha y hora de inicio son obligatorias") LocalDateTime inicio,

        @Size(max = 300, message = "La nota no puede superar 300 caracteres") String nota) {
}