package com.devsenior.sillon.servicio.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearServicioRequest(
        @NotBlank(message = "El nombre del servicio es obligatorio")
        @Size(max = 80, message = "El nombre no debe superar 80 caracteres")
        String nombre,

        @Size(max = 300, message = "La descripción no debe superar 300 caracteres")
        String descripcion,

        @NotNull(message = "La duración en minutos es obligatoria")
        @Min(value = 15, message = "La duración mínima es 15 minutos")
        @Max(value = 480, message = "La duración máxima es 480 minutos")
        Integer duracionMinutos,

        @NotNull(message = "El precio es obligatorio")
        @Min(value = 1, message = "El precio debe ser un número entero mayor que cero")
        Integer precio,

        Boolean activo
) {
}
