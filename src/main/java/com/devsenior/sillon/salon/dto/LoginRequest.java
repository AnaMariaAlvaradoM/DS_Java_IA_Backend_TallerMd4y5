package com.devsenior.sillon.salon.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El formato del correo es inválido") String email,

        @NotBlank(message = "La contraseña es obligatoria") String password) {
}