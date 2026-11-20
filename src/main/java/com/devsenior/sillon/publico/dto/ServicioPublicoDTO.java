package com.devsenior.sillon.publico.dto;

public record ServicioPublicoDTO(
        Long id,
        String nombre,
        String descripcion,
        int duracionMinutos,
        int precio
) {
}
