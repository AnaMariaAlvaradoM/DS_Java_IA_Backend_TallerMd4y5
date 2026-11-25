package com.devsenior.sillon.servicio;

/** DTO de respuesta para un servicio del catalogo. */
public record ServicioRespuesta(
        Long id,
        String nombre,
        String descripcion,
        int duracionMinutos,
        int precio,
        boolean activo) {
}
