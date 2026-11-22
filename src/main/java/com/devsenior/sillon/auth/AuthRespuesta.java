package com.devsenior.sillon.auth;

import com.devsenior.sillon.usuario.Rol;

/** Respuesta del registro y del login (mismo objeto en ambos). */
public record AuthRespuesta(
        String token,
        String tipo,
        long expiraEnMinutos,
        Long usuarioId,
        String nombre,
        Rol rol,
        Long salonId,
        String salonSlug) {
}
