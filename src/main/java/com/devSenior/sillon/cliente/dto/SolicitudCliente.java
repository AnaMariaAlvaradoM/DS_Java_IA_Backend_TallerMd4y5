package com.devSenior.sillon.cliente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudCliente(
		@NotBlank @Size(max = 100) String nombre,
		@NotBlank @Size(min = 7, max = 30) String telefono) {
}
