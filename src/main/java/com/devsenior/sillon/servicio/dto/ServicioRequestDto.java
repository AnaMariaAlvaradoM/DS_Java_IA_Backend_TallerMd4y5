package com.devsenior.sillon.servicio.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ServicioRequestDto(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no debe superar los 80 caracteres")
        String nombre,

        @Size(max = 300, message = "La descripcion no debe superar los 300 caracteres")
        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @Min(value = 1, message = "El precio debe ser un entero mayor a cero")
        Integer precio,

        @NotNull(message = "La duracion es obligatoria")
        @Min(value = 15, message = "La duracion minima es de 15 minutos")
        @Max(value = 480, message = "La duracion maxima es de 480 minutos")
        Integer duracionMinutos,

        Boolean activo
) {}
