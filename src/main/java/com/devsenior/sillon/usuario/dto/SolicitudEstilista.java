package com.devsenior.sillon.usuario.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudEstilista(

		@NotBlank
		@Size(max = 100)
		String nombre,

		@NotBlank
		@Email
		@Size(max = 255)
		String email,

		@NotBlank
		@Size(min = 8, max = 72)
		String password,

		@NotEmpty
		List<@NotNull Long> servicioIds

) {
}
