package com.devsenior.sillon.servicio;

public record ServicioRespuesta(
        Long id,
        String nombre,
        String descripcion,
        int duracionMinutos,
        Long precio,
        boolean activo) {
}
