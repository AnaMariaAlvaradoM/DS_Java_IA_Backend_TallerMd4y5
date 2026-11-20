package com.devsenior.sillon.cita;

import jakarta.validation.constraints.NotNull;

public record CambioEstadoSolicitud(@NotNull EstadoCita estado) {
}
