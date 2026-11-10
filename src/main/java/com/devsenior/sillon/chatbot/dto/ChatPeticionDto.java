package com.devsenior.sillon.chatbot.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChatPeticionDto(
        @NotBlank @Size(max = 120) String clienteNombre,
        @NotBlank @Size(max = 120) String clienteContacto,
        @NotNull @Size(min = 1, max = 20) List<@Valid MensajeChatDto> historial
) {}
