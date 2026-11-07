package com.devsenior.sillon.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record EstilistaSolicitud(@NotBlank String nombre, @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotEmpty List<@NotNull Long> servicioIds) {
}
