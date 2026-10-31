package com.devsenior.sillon.auth.dto;

public record AuthResponseDto(
        String token,
        String tipo,
        long expiraEnMinutos,
        Long usuarioId,
        String nombre,
        String rol,
        Long salonId,
        String salonSlug
) {}