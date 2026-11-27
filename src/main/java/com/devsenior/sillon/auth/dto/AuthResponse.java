package com.devsenior.sillon.auth.dto;

public record AuthResponse(
        String token,
        String tipo,
        long usuarioId,
        String email,
        String rol,
        Long salonId,
        String salonSlug,
        Long expiraEnMinutos
) {
    public static AuthResponse of(String token, String email, String rol, Long usuarioId, Long salonId, String salonSlug, Long expiraEnMinutos) {
        return new AuthResponse(token, "Bearer", usuarioId, email, rol, salonId, salonSlug, expiraEnMinutos);
    }
}
