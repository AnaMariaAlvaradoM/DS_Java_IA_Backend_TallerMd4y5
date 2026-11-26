package com.devsenior.sillon.salon;

import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConfiguracionPeticion(
        @NotNull(message = "es obligatoria") LocalTime horaApertura,
        @NotNull(message = "es obligatoria") LocalTime horaCierre,
        @NotNull(message = "es obligatorio") Boolean abreDomingos,
        @NotNull(message = "es obligatorio") @Min(value = 15, message = "debe ser mínimo 15") @Max(value = 60, message = "debe ser máximo 60") Integer intervaloMinutos) {
}
