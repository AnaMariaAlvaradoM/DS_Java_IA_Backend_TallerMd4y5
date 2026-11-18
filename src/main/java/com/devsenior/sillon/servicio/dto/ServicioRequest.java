package com.devsenior.sillon.servicio.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/servicios y de PUT /api/servicios/{id}: el contrato
 * define que el PUT recibe lo mismo que el POST (SPEC.md §7.7).
 */
public record ServicioRequest(
        @NotBlank(message = "es obligatorio") @Size(max = 80, message = "máximo 80 caracteres") String nombre,
        @Size(max = 300, message = "máximo 300 caracteres") String descripcion,
        @NotNull(message = "es obligatoria") @Min(value = 15, message = "debe ser de 15 a 480 minutos") @Max(value = 480, message = "debe ser de 15 a 480 minutos") Integer duracionMinutos,
        @NotNull(message = "es obligatorio") @Positive(message = "debe ser un entero mayor que cero") Integer precio,
        Boolean activo) {

    /** Si activo no llega, el servicio nace activo (SPEC.md §7.7). */
    public boolean activoODefecto() {
        return activo == null || activo;
    }
}
