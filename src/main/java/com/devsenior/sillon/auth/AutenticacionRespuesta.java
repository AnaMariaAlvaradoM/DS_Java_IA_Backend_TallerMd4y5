package com.devsenior.sillon.auth;

public record AutenticacionRespuesta(
        String token,
        String tipo,
        int expiraEnMinutos,
        Long usuarioId,
        String nombre,
        String rol,
        Long salonId,
        String salonSlug) {
}
