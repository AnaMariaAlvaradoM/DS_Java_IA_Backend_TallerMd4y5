package com.devsenior.sillon.cita.dto;

import com.devsenior.sillon.cita.model.EstadoCita;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoRequest(@NotNull(message = "es obligatorio") EstadoCita estado) {
}
