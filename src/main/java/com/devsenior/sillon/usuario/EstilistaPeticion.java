package com.devsenior.sillon.usuario;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record EstilistaPeticion(
        @NotBlank(message = "es obligatorio") @Size(max = 100, message = "máximo 100 caracteres") String nombre,
        @NotBlank(message = "es obligatorio") @Email(message = "no tiene un formato válido") String email,
        @NotBlank(message = "es obligatoria") @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres") String password,
        @NotEmpty(message = "debe tener al menos un servicio") List<Long> servicioIds) {
}
