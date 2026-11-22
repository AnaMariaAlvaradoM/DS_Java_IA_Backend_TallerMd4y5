package com.devsenior.sillon.servicio;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Peticion para crear o actualizar un servicio del catalogo.
 * La duracion va de 15 a 480 minutos, y el precio es un numero entero en pesos colombianos mayor que cero.
 */
public record ServicioCreacionPeticion(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
        String nombre,

        @Size(max = 300, message = "La descripción no puede superar los 300 caracteres")
        String descripcion,

        @NotNull(message = "La duración es obligatoria")
        @Min(value = 15, message = "La duración mínima es de 15 minutos")
        @Max(value = 480, message = "La duración máxima es de 480 minutos")
        Integer duracionMinutos,

        @NotNull(message = "El precio es obligatorio")
        @Min(value = 1, message = "El precio debe ser un número entero mayor que cero")
        Integer precio,

        Boolean activo) {
}
