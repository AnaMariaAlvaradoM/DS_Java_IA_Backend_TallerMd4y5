package com.devsenior.sillon.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank(message = "El nombre del dueño es obligatorio")
        String nombre,

        @NotBlank(message = "El nombre del salón es obligatorio")
        @Size(max = 100, message = "El nombre del salón no puede exceder los 100 caracteres")
        String nombreSalon,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email,

        @NotBlank(message = "La ciudad es obligatoria")
        @Size(max = 60, message = "La ciudad no puede exceder los 60 caracteres")
        String ciudad,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8,max = 72, message = "La contraseña debe tener al menos 8 caracteres")
        String password,
        @NotBlank(message = "El numero es obligatorio")
        String telefono,
        @NotBlank(message = "La direccion es obligatoria")
        @Size(max = 150, message = "La dirección no puede exceder los 150 caracteres")
        String direccion

) {
}
