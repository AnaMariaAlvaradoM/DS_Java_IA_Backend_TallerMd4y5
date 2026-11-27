package com.devsenior.sillon.auth.dto;

public record RegistroResponse(
        Long salonId,
        Long usuarioId,
        String nombreSalon,
        String email,
        String rol,
        String mensaje
) {
}
