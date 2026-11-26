package com.devsenior.sillon.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginPeticion(
        @NotBlank(message = "es obligatorio") String email,
        @NotBlank(message = "es obligatoria") String password) {
}
