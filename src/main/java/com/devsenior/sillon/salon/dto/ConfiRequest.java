package com.devsenior.sillon.salon.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalTime;

public record ConfiRequest(
        @NotNull(message = "La hora de apertura es obligatoria")
        LocalTime horaApertura,

        @NotNull(message = "La hora de cierre es obligatoria")
        LocalTime horaCierre,

        @NotNull(message = "El intervalo en minutos es obligatorio")
        @Positive(message = "El intervalo debe ser un número entero mayor a cero")
        Integer intervaloMinutos,

        @NotNull(message = "Debe especificar si abre los domingos")
        Boolean abreDomingo
) {
}
