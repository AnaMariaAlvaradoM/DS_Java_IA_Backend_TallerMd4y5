package com.devsenior.sillon.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Cuerpo de POST /api/auth/login (SPEC.md §7.5). */
public record LoginRequest(
        @NotBlank(message = "es obligatorio") String email,
        @NotBlank(message = "es obligatoria") String password) {
}
