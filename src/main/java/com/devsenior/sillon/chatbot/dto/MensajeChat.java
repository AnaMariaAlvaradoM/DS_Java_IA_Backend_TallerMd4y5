package com.devsenior.sillon.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MensajeChat(

        @NotBlank(message = "El rol es obligatorio")
        @Pattern(regexp = "^(CLIENTE|ASISTENTE)$", message = "El rol debe ser CLIENTE o ASISTENTE")
        String rol,

        @NotBlank(message = "El texto es obligatorio")
        @Size(max = 500, message = "El mensaje no puede superar los 500 caracteres")
        String texto
) {
}
