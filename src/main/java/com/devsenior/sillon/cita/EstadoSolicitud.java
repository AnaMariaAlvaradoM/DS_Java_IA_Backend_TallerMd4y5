package com.devsenior.sillon.cita;

import jakarta.validation.constraints.NotNull;

public record EstadoSolicitud(
        @NotNull EstadoCita estado) {
}
