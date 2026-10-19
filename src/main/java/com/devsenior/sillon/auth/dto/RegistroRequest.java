package com.devsenior.sillon.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank(message = "El nombre del dueño es obligatorio")
        String nombre,

        @NotBlank(message = "El nombre del salón es obligatorio")
        String nombreSalon,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8,max = 72, message = "La contraseña debe tener al menos 8 caracteres")
        String contrasena
) {
}
