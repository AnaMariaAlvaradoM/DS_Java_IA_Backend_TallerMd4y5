package com.devsenior.sillon.cita;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequest(@NotNull EstadoCita estado) {
}
