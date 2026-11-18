package com.devsenior.sillon.salon.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Cuerpo de PUT /api/salon/configuracion: los cuatro campos son obligatorios (SPEC.md §7.6). */
public record ConfiguracionRequest(
        @NotNull(message = "es obligatoria") LocalTime horaApertura,
        @NotNull(message = "es obligatoria") LocalTime horaCierre,
        @NotNull(message = "es obligatorio") Boolean abreDomingos,
        @NotNull(message = "es obligatorio") @Min(value = 15, message = "debe ser de 15 a 60 minutos") @Max(value = 60, message = "debe ser de 15 a 60 minutos") Integer intervaloMinutos) {
}
