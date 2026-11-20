package com.devsenior.sillon.servicio.dto;

import java.util.Objects;

public record ServicioRespuesta(
        Long id,
        String nombre,
        String descripcion,
        int duracionMinutos,
        int precio,
        boolean activo
) {
}
