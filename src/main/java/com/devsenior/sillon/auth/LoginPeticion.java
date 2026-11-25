package com.devsenior.sillon.auth;

import jakarta.validation.constraints.NotBlank;

/** Cuerpo de POST /api/auth/login. Sin @Email a proposito: un correo mal escrito tambien responde 401 generico. */
public record LoginPeticion(
        @NotBlank(message = "es obligatorio") String email,
        @NotBlank(message = "es obligatoria") String password) {
}
