package com.devsenior.sillon.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank @Size(max = 100) String salonNombre,
        @NotBlank @Size(max = 60) String ciudad,
        @NotBlank @Size(max = 150) String direccion,
        @NotBlank @Size(max = 30) String telefono,
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password) {

    public RegistroRequest {
        email = email == null ? null : email.trim().toLowerCase();
    }
}
