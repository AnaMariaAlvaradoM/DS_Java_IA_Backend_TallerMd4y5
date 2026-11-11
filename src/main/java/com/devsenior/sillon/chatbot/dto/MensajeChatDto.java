package com.devsenior.sillon.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MensajeChatDto(
        @NotNull RolChat rol,
        @NotBlank @Size(max = 500) String texto
) {}
