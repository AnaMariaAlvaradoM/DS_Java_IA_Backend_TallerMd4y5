package com.devsenior.sillon.usuario.dto;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EstilistaRequest(
        @NotBlank(message = "es obligatorio") String nombre,
        @NotBlank(message = "es obligatorio") @Email(message = "debe tener un formato válido") String email,
        @NotBlank(message = "es obligatoria")
        @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres") String password,
        @NotEmpty(message = "debe contener al menos un id") List<@NotNull(message = "el id es obligatorio") Long> servicioIds) {

    @JsonIgnore
    @AssertTrue(message = "password no debe superar 72 bytes en UTF-8 para BCrypt")
    public boolean isPasswordCompatibleConBCrypt() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
