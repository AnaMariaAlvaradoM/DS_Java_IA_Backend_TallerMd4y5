package com.devsenior.sillon.auth;

import com.devsenior.sillon.usuario.Rol;

public record AuthResponse(String token, String tipo, long expiraEnMinutos, Long usuarioId,
                           String nombre, Rol rol, Long salonId, String salonSlug) {
}
