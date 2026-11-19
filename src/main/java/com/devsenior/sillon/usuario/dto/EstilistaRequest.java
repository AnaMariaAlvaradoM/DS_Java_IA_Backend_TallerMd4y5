package com.devsenior.sillon.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record EstilistaRequest(
        @NotBlank(message = "es obligatorio") @Size(max = 100, message = "máximo 100 caracteres") String nombre,
        @NotBlank(message = "es obligatorio") @Email(message = "debe ser un correo válido") String email,
        @NotBlank(message = "es obligatoria") @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres") String password,
        @NotEmpty(message = "debe tener al menos un servicio") List<@NotNull(message = "no puede tener ids vacíos") Long> servicioIds
) {
}
