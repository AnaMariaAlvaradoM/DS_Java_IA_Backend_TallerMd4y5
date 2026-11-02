package com.devsenior.sillon.cita.dto;

import com.devsenior.sillon.cita.EstadoCita;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoRequestDto(
        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoCita estado
) {}
