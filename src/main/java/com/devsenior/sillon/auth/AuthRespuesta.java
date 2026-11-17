package com.devsenior.sillon.auth;

import com.devsenior.sillon.usuario.Rol;

public record AuthRespuesta(
        String token,
        String tipo,
        int expiraEnMinutos,
        Long usuarioId,
        String nombre,
        Rol rol,
        Long salonId,
        String salonSlug) {
}
