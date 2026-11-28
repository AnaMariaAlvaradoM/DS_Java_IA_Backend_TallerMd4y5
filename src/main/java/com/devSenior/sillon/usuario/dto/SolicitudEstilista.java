package com.devSenior.sillon.usuario.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudEstilista(
		@NotBlank @Size(max = 100) String nombre,
		@NotBlank @Email String email,
		@NotBlank @Size(min = 8, max = 72) String password,
		@NotNull @Size(min = 1) List<Long> servicioIds) {
}
