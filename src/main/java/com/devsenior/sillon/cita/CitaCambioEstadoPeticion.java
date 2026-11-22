package com.devsenior.sillon.cita;

import jakarta.validation.constraints.NotNull;

/** Peticion para cambiar el estado de una cita existente. */
public record CitaCambioEstadoPeticion(
        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoCita estado) {
}
