package com.devsenior.sillon.salon.dto;

import com.devsenior.sillon.usuario.modelo.Rol;

public record AuthResponse(
        String token,
        String tipo,
        long expiraEnMinutos,
        Long usuarioId,
        String nombre,
        Rol rol,
        Long salonId,
        String salonSlug) {
}
