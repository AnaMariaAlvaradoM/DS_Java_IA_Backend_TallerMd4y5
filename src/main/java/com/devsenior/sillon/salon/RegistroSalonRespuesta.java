package com.devsenior.sillon.salon;

import com.devsenior.sillon.usuario.Rol;

public record RegistroSalonRespuesta(
        Long salonId,
        String nombreSalon,
        String slug,
        String email,
        Rol rol,
        String token
) {
}