package com.devsenior.sillon.servicio.dto;
//201 con id, nombre, descripcion,
//duracionMinutos, precio, activo · 400
public record ServicioResponse(
        Long id,
        String nombre,
        String descripcion,
        Integer duracionMinutos,
        Integer precio,
        Boolean activo
) {
}
