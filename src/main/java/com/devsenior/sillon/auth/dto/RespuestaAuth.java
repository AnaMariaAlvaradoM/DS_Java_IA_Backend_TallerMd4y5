package com.devsenior.sillon.auth.dto;

import com.devsenior.sillon.usuario.modelo.Rol;

public record RespuestaAuth(

        String token,
        String tipo,
        int expiraEnMinutos,
        Long usuarioId,
        String nombre,
        Rol rol,
        Long salonId,
        String salonSlug

) {
}