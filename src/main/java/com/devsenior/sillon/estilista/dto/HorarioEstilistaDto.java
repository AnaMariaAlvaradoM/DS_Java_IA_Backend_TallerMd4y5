package com.devsenior.sillon.estilista.dto;

import com.devsenior.sillon.estilista.DiaSemana;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record HorarioEstilistaDto(
        @NotNull(message = "El dia de la semana es obligatorio")
        DiaSemana diaSemana,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicio,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime horaFin
) {}
