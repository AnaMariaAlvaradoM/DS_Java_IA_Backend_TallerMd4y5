package com.devsenior.sillon.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ChatSolicitud(@NotBlank String clienteNombre,
                            @NotBlank @Size(max = 120) String clienteContacto,
                            @NotNull @Size(min = 1, max = 20) List<@NotNull @Valid TurnoChatSolicitud> historial) { }
