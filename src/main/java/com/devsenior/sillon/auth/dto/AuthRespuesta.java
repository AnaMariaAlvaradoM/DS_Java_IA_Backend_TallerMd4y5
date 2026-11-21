package com.devsenior.sillon.auth.dto;

import com.devsenior.sillon.usuario.Rol;

public record AuthRespuesta(
        String token,
        String tipo,
        long expiraEnMinutos,
        Long usuarioId,
        String nombre,
        Rol rol,
        Long salonId,
        String salonSlug
) {
}
