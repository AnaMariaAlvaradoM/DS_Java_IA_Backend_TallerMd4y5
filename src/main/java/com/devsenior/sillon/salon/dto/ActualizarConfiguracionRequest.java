package com.devsenior.sillon.salon.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record ActualizarConfiguracionRequest(
        @NotNull(message = "La hora de apertura es obligatoria")
        LocalTime horaApertura,

        @NotNull(message = "La hora de cierre es obligatoria")
        LocalTime horaCierre,

        @NotNull(message = "Debe indicar si abre domingos")
        Boolean abreDomingos,

        @NotNull(message = "El intervalo de minutos es obligatorio")
        @Min(value = 15, message = "El intervalo de minutos debe ser de al menos 15")
        @Max(value = 60, message = "El intervalo de minutos no debe superar 60")
        Integer intervaloMinutos
) {
}
