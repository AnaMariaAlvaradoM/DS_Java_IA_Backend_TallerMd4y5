package com.devsenior.sillon.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record RegistroRequest(
        @NotBlank(message = "es obligatorio") @Size(max = 100, message = "máximo 100 caracteres") String salonNombre,
        @NotBlank(message = "es obligatoria") @Size(max = 60, message = "máximo 60 caracteres") String ciudad,
        @NotBlank(message = "es obligatoria") @Size(max = 150, message = "máximo 150 caracteres") String direccion,
        @NotBlank(message = "es obligatorio") @Size(max = 30, message = "máximo 30 caracteres") String telefono,
        @NotBlank(message = "es obligatorio") @Size(max = 100, message = "máximo 100 caracteres") String nombre,
        @NotBlank(message = "es obligatorio") @Email(message = "debe ser un correo válido") String email,
        @NotBlank(message = "es obligatorio") @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres") String password) {
    public RegistroRequest {
        if (email != null) {
            email = email.strip().toLowerCase(Locale.ROOT);
        }
    }
}
