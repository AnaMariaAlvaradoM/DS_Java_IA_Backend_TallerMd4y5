package com.devsenior.sillon.salon.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

/** Todos obligatorios. Que la apertura sea antes del cierre es regla de negocio (409), no de formato. */
public record ConfiguracionRequest(
        @NotNull(message = "es obligatoria") LocalTime horaApertura,
        @NotNull(message = "es obligatoria") LocalTime horaCierre,
        @NotNull(message = "es obligatorio") Boolean abreDomingos,
        @NotNull(message = "es obligatorio")
        @Min(value = 15, message = "debe estar entre 15 y 60")
        @Max(value = 60, message = "debe estar entre 15 y 60") Integer intervaloMinutos
) {
}
