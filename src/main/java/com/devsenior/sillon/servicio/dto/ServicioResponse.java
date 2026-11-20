package com.devsenior.sillon.servicio.dto;

/** Respuesta del contrato: id, nombre, descripcion, duracionMinutos, precio, activo (SPEC.md §7.7). */
public record ServicioResponse(
        Long id,
        String nombre,
        String descripcion,
        int duracionMinutos,
        int precio,
        boolean activo) {
}
