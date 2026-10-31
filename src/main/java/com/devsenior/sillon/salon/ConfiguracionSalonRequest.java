package com.devsenior.sillon.salon;

import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConfiguracionSalonRequest(
        @NotNull LocalTime horaApertura,
        @NotNull LocalTime horaCierre,
        @NotNull Boolean abreDomingos,
        @NotNull @Min(15) @Max(60) Integer intervaloMinutos) {
}
