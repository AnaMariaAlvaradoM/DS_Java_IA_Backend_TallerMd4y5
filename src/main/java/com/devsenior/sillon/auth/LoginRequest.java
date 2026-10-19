package com.devsenior.sillon.auth;

import jakarta.validation.constraints.NotBlank;
import java.util.Locale;

public record LoginRequest(@NotBlank(message = "es obligatorio") String email,
                           @NotBlank(message = "es obligatorio") String password) {
    public LoginRequest {
        if (email != null) {
            email = email.strip().toLowerCase(Locale.ROOT);
        }
    }
}
