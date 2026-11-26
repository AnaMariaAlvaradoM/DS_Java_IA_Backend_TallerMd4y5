package com.devsenior.sillon.servicio;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ServicioPeticion(
        @NotBlank(message = "es obligatorio") @Size(max = 80, message = "máximo 80 caracteres") String nombre,
        @Size(max = 300, message = "máximo 300 caracteres") String descripcion,
        @NotNull(message = "es obligatoria") @Min(value = 15, message = "debe ser mínimo 15 minutos") @Max(value = 480, message = "debe ser máximo 480 minutos") Integer duracionMinutos,
        @NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor que cero") Integer precio,
        Boolean activo) {
}
