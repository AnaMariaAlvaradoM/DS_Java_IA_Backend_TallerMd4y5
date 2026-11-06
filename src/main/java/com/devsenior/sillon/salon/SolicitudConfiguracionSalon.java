
package com.devsenior.sillon.salon;

import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SolicitudConfiguracionSalon(

        @NotNull(message = "La hora de apertura es obligatoria")
        LocalTime horaApertura,

        @NotNull(message = "La hora de cierre es obligatoria")
        LocalTime horaCierre,

        @NotNull(message = "Debe indicar si abre los domingos")
        Boolean abreDomingos,

        @NotNull(message = "El intervalo es obligatorio")
        @Min(value = 15, message = "El intervalo mínimo es de 15 minutos")
        @Max(value = 60, message = "El intervalo máximo es de 60 minutos")
        Integer intervaloMinutos

) {
}
