
package com.devsenior.sillon.autenticacion;

import com.devsenior.sillon.usuario.Rol;

public record RespuestaAutenticacion(
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
