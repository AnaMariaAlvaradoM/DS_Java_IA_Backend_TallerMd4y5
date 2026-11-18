package com.devsenior.sillon.usuario.estilista.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EstilistaRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email
        String email,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8,max = 72, message = "La contraseña debe tener al menos 8 caracteres")
        String password,
        @NotNull(message = "Debe seleccionar al menos un servicio")
        List<Long>serviciosId
) {
}
