package com.devsenior.sillon.publico;

public record ServicioPublicoRespuesta(
        Long id,
        String nombre,
        String descripcion,
        int duracionMinutos,
        Long precio) {
}
