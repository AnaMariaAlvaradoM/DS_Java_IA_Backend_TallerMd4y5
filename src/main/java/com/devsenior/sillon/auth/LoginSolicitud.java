package com.devsenior.sillon.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginSolicitud(
        @NotBlank String email,
        @NotBlank String password) {
}
