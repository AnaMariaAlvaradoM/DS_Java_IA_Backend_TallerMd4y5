package com.devsenior.sillon.auth.dto;

/** Respuesta del registro y del login, con los campos exactos del contrato (SPEC.md §7.5). */
public record RespuestaAuth(
        String token,
        String tipo,
        long expiraEnMinutos,
        Long usuarioId,
        String nombre,
        String rol,
        Long salonId,
        String salonSlug) {
}
