package com.devsenior.sillon.cita.dto;

import java.time.LocalDateTime;

import com.devsenior.sillon.cita.modelo.EstadoCita;
import com.devsenior.sillon.cita.modelo.OrigenCita;
import com.fasterxml.jackson.annotation.JsonFormat;

public record RespuestaCita(

		Long id,

		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
		LocalDateTime inicio,

		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
		LocalDateTime fin,

		EstadoCita estado,
		OrigenCita origen,
		int precio,
		Long servicioId,
		String servicioNombre,
		Long estilistaId,
		String estilistaNombre,
		String clienteNombre,
		String clienteTelefono,
		String nota,

		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
		LocalDateTime fechaCreacion,

		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
		LocalDateTime fechaActualizacion

) {
}
