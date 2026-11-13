package com.devsenior.sillon.servicio;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServicioSolicitud(
        @NotBlank @Size(max = 80) String nombre,
        @Size(max = 300) String descripcion,
        @NotNull @Min(15) @Max(480) Integer duracionMinutos,
        @NotNull @Min(1) Long precio,
        Boolean activo) {
}
