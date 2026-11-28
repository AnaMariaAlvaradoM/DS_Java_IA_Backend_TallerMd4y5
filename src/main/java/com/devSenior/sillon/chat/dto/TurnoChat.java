package com.devSenior.sillon.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TurnoChat(
		@NotNull RolConversacion rol,
		@NotBlank @Size(max = 500) String texto) {
}
