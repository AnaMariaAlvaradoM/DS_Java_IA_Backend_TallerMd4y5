package com.devsenior.sillon.cita;

import jakarta.validation.constraints.NotNull;

public record EstadoCitaRequest(@NotNull(message = "es obligatorio") EstadoCita estado) { }
