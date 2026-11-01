package com.devsenior.sillon.salon.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank(message = "El nombre del salón es obligatorio") @Size(max = 100, message = "El nombre del salón no puede superar 100 caracteres") String salonNombre,

        @NotBlank(message = "La ciudad es obligatoria") @Size(max = 60, message = "La ciudad no puede superar 60 caracteres") String ciudad,

        @NotBlank(message = "La dirección es obligatoria") @Size(max = 150, message = "La dirección no puede superar 150 caracteres") String direccion,

        @NotBlank(message = "El teléfono es obligatorio") @Size(max = 30, message = "El teléfono no puede superar 30 caracteres") String telefono,

        @NotBlank(message = "El nombre del dueño es obligatorio") @Size(max = 100, message = "El nombre no puede superar 100 caracteres") String nombre,

        @NotBlank(message = "El correo es obligatorio") @Email(message = "El formato del correo es inválido") String email,

        @NotBlank(message = "La contraseña es obligatoria") @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String password) {
}
