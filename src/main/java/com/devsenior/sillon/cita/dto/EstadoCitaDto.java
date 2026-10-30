package com.devsenior.sillon.cita.dto;

import com.devsenior.sillon.cita.model.EstadoCita;
import jakarta.validation.constraints.NotNull;

public record EstadoCitaDto(
        @NotNull(message = "El estado es obligatorio")
        EstadoCita estado
) {
}
