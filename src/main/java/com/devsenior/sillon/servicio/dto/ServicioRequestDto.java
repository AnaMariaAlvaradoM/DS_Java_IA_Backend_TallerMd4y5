package com.devsenior.sillon.servicio.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ServicioRequestDto(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @Min(value = 0, message = "El precio debe ser positivo")
        BigDecimal precio,

        @NotNull(message = "La duracion es obligatoria")
        @Min(value = 5, message = "La duracion minima es de 5 minutos")
        Integer duracionMinutos
) {}
