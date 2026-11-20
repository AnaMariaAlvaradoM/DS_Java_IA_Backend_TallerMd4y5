package com.devsenior.sillon.salon.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record ConfiguracionSalonRequest(
        @NotNull(message = "La hora de apertura es obligatoria") LocalTime horaApertura,

        @NotNull(message = "La hora de cierre es obligatoria") LocalTime horaCierre,

        @NotNull(message = "El campo abreDomingos es obligatorio") Boolean abreDomingos,

        @NotNull(message = "El intervalo en minutos es obligatorio") @Min(value = 15, message = "El intervalo mínimo es de 15 minutos") @Max(value = 60, message = "El intervalo máximo es de 60 minutos") Integer intervaloMinutos) {
}