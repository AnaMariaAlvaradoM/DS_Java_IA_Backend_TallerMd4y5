package com.devSenior.sillon.salon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudSalon(
		@NotBlank @Size(max = 100) String nombre,
		@NotBlank @Size(max = 60) String ciudad,
		@NotBlank @Size(max = 150) String direccion,
		@NotBlank @Size(max = 30) String telefono) {
}
