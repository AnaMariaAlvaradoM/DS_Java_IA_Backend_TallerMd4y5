package com.devsenior.sillon.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;

public record EstilistaRequest(
        @NotBlank(message = "es obligatorio") @Size(max = 100, message = "máximo 100 caracteres") String nombre,
        @NotBlank(message = "es obligatorio") @Email(message = "debe ser un correo válido") String email,
        @NotBlank(message = "es obligatorio") @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres") String password,
        @NotEmpty(message = "debe incluir al menos un servicio") List<@NotNull(message = "no puede ser nulo") Long> servicioIds) {
    public EstilistaRequest {
        if (email != null) { email = email.strip().toLowerCase(Locale.ROOT); }
    }
}
