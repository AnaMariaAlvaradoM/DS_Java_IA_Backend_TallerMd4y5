package com.devsenior.sillon.usuario;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record EstilistaRequest(
        @NotBlank String nombre,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotEmpty List<Long> servicioIds) {

    public EstilistaRequest {
        email = email == null ? null : email.trim().toLowerCase();
    }
}
