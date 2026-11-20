package com.devsenior.sillon.servicio.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServicioRequest(
        @NotBlank(message = "El nombre del servicio es obligatorio") @Size(max = 80, message = "El nombre no puede superar 80 caracteres") String nombre,

        @Size(max = 300, message = "La descripción no puede superar 300 caracteres") String descripcion,

        @NotNull(message = "La duración en minutos es obligatoria") @Min(value = 15, message = "La duración mínima es de 15 minutos") @Max(value = 480, message = "La duración máxima es de 480 minutos") Integer duracionMinutos,

        @NotNull(message = "El precio es obligatorio") @Min(value = 1, message = "El precio debe ser mayor que 0") Integer precio,

        Boolean activo) {
}