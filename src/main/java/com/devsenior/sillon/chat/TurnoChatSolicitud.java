package com.devsenior.sillon.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TurnoChatSolicitud(@NotNull RolTurno rol,
                                 @NotBlank @Size(max = 500) String texto) { }
