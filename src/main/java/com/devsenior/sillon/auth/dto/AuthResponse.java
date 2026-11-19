package com.devsenior.sillon.auth.dto;

import com.devsenior.sillon.usuario.model.Rol;
import com.devsenior.sillon.usuario.model.Usuario;

/** Respuesta de registro y login. Nunca devuelve la entidad Usuario. */
public record AuthResponse(
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
