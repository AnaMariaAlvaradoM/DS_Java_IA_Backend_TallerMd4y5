package com.devsenior.sillon.servicio.dto;

import jakarta.validation.constraints.*;

public record ServicioRequest(
        @NotBlank(message = "El nombre del servicio es obligatorio")
    String nombre,
    String descripcion,

    @NotNull(message = "La duración del servicio es obligatoria")
    @Min(value = 15, message = "La duración del servicio no puede ser menor a 15 minutos")
    @Max(value = 480, message = "La duración del servicio no puede ser mayor a 480 minutos")
    Integer duracionMinutos,

    @NotNull(message = "El precio del servicio es obligatorio")
    @Positive(message = "El precio del servicio debe ser un número positivo")
    Integer precio,
    Boolean activo
) {
}
