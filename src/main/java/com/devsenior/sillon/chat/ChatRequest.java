package com.devsenior.sillon.chat;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank @Size(max = 120) String clienteNombre,
        @NotBlank @Size(max = 120) String clienteContacto,
        @NotNull @Size(min = 1, max = 20) @Valid List<TurnoChat> historial) {
}
