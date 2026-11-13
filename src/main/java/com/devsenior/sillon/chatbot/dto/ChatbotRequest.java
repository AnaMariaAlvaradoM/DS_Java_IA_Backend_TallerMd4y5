package com.devsenior.sillon.chatbot.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChatbotRequest(
        @NotBlank(message = "El nombre del cliente es obligatorio")
        String clienteNombre,

        @NotBlank(message = "El contacto es obligatorio")
        @Size(max = 120, message = "El contacto no puede superar los 120 caracteres")
        String clienteContacto,

        @NotNull(message = "El historial es obligatorio")
        @Size(min = 1, max = 20, message = "Debe enviar entre 1 y 20 mensajes")
        @Valid // Obliga a validar cada mensaje por dentro
        List<MensajeChat> historial
) {
}
