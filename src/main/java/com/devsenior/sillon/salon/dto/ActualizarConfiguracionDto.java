package com.devsenior.sillon.salon.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record ActualizarConfiguracionDto(
        @NotNull LocalTime horaApertura,
        @NotNull LocalTime horaCierre,
        boolean abreDomingos,
        @Min(5) int intervaloMinutos
) {}