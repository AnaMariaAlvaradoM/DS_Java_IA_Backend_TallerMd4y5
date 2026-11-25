package com.devsenior.sillon.cliente;

/** DTO de respuesta para un cliente del salon. */
public record ClienteRespuesta(
        Long id,
        String nombre,
        String telefono) {
}
