package com.devSenior.sillon.cita.dto;

import java.time.LocalDateTime;

import com.devSenior.sillon.cita.entidad.EstadoCita;
import com.devSenior.sillon.cita.entidad.OrigenCita;
import com.fasterxml.jackson.annotation.JsonFormat;

public record CitaRespuesta(
		Long id,
		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime inicio,
		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime fin,
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
		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime fechaCreacion,
		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime fechaActualizacion) {
}
