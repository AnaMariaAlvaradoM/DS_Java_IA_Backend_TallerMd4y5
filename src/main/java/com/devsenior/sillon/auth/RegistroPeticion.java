package com.devsenior.sillon.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de POST /api/auth/registro. direccion y telefono son opcionales (el contrato solo les pone maximo). */
public record RegistroPeticion(
        @NotBlank(message = "es obligatorio")
        @Size(max = 100, message = "debe tener máximo 100 caracteres")
        String salonNombre,

        @NotBlank(message = "es obligatoria")
        @Size(max = 60, message = "debe tener máximo 60 caracteres")
        String ciudad,

        @Size(max = 150, message = "debe tener máximo 150 caracteres")
        String direccion,

        @Size(max = 30, message = "debe tener máximo 30 caracteres")
        String telefono,

        @NotBlank(message = "es obligatorio")
        @Size(max = 100, message = "debe tener máximo 100 caracteres")
        String nombre,

        @NotBlank(message = "es obligatorio")
        @Email(message = "no tiene un formato de correo válido")
        @Size(max = 150, message = "debe tener máximo 150 caracteres")
        String email,

        @NotBlank(message = "es obligatoria")
        @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres")
        String password) {
}
