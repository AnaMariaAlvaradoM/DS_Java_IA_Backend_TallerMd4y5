package com.devsenior.sillon.salon;

import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Cuerpo de PUT /api/salon/configuracion. Los cuatro campos son obligatorios. Las horas llegan como HH:mm. */
public record ConfiguracionPeticion(
        @NotNull(message = "es obligatoria") LocalTime horaApertura,
        @NotNull(message = "es obligatoria") LocalTime horaCierre,
        @NotNull(message = "es obligatorio") Boolean abreDomingos,
        @NotNull(message = "es obligatorio")
        @Min(value = 15, message = "debe estar entre 15 y 60")
        @Max(value = 60, message = "debe estar entre 15 y 60")
        Integer intervaloMinutos) {
}
