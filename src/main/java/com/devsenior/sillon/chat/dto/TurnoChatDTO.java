package com.devsenior.sillon.chat.dto;

import com.devsenior.sillon.chat.RolMensajeChat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TurnoChatDTO(
        @NotNull(message = "El rol del turno es obligatorio")
        RolMensajeChat rol,

        @NotBlank(message = "El texto del mensaje es obligatorio")
        @Size(max = 500, message = "El mensaje no debe superar 500 caracteres")
        String texto
) {
}
