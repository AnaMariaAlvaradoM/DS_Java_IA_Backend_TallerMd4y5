package com.devsenior.sillon.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank(message = "El nombre del salón es obligatorio")
        @Size(max = 100, message = "El nombre del salón no debe superar 100 caracteres")
        String salonNombre,

        @NotBlank(message = "La ciudad es obligatoria")
        @Size(max = 60, message = "La ciudad no debe superar 60 caracteres")
        String ciudad,

        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 150, message = "La dirección no debe superar 150 caracteres")
        String direccion,

        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 30, message = "El teléfono no debe superar 30 caracteres")
        String telefono,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no debe superar 100 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo debe tener un formato válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password
) {
}
