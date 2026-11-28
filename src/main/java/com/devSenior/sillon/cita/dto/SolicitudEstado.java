package com.devSenior.sillon.cita.dto;

import com.devSenior.sillon.cita.entidad.EstadoCita;

import jakarta.validation.constraints.NotNull;

public record SolicitudEstado(@NotNull EstadoCita estado) {
}
