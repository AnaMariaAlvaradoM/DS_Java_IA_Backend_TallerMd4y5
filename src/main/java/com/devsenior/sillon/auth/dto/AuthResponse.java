package com.devsenior.sillon.auth.dto;

public record AuthResponse(
        String token,
        String tipo,
        String email,
        String rol,
        Long salonId,
        String slug
) {
    public static AuthResponse of(String token, String email, String rol, Long salonId, String slug) {
        return new AuthResponse(token, "Bearer", email, rol, salonId, slug);
    }
}
