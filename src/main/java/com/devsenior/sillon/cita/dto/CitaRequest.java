package com.devsenior.sillon.cita.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** Mismo cuerpo para la cita del salón (POST /api/citas) y la reserva en línea. */
public record CitaRequest(
        @NotBlank(message = "es obligatorio") @Size(max = 100, message = "máximo 100 caracteres") String clienteNombre,
        @NotBlank(message = "es obligatorio") @Size(min = 7, max = 30, message = "debe tener entre 7 y 30 caracteres") String clienteTelefono,
        @NotNull(message = "es obligatorio") Long servicioId,
        @NotNull(message = "es obligatorio") Long estilistaId,
        @NotNull(message = "es obligatorio") LocalDateTime inicio,
        @Size(max = 300, message = "máximo 300 caracteres") String nota
) {
}
