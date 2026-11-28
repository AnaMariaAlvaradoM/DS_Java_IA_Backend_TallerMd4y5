package com.devSenior.sillon.chat.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record SolicitudChat(
		@NotBlank @Size(max = 100) String clienteNombre,
		@NotBlank @Size(max = 120) String clienteContacto,
		@NotEmpty @Size(max = 20) List<@Valid TurnoChat> historial) {
}
