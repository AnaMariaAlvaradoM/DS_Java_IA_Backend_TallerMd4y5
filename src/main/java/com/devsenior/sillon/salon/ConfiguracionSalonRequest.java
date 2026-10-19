package com.devsenior.sillon.salon;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record ConfiguracionSalonRequest(
        @NotNull(message = "es obligatoria") LocalTime horaApertura,
        @NotNull(message = "es obligatoria") LocalTime horaCierre,
        @NotNull(message = "es obligatorio") Boolean abreDomingos,
        @NotNull(message = "es obligatorio") @Min(value = 15, message = "mínimo 15")
        @Max(value = 60, message = "máximo 60") Integer intervaloMinutos) {
}
