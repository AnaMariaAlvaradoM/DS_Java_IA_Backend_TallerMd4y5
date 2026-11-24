package com.devsenior.sillon.cita.dto;

import com.devsenior.sillon.cita.modelo.EstadoCita;

import jakarta.validation.constraints.NotNull;

public record SolicitudCambiarEstadoCita(

		@NotNull
		EstadoCita estado

) {
}
