package com.devSenior.sillon.chat.dto;

import java.time.LocalDateTime;

import com.devSenior.sillon.chat.entidad.EstadoSolicitud;
import com.devSenior.sillon.chat.entidad.MotivoSolicitud;
import com.fasterxml.jackson.annotation.JsonFormat;

public record SolicitudRespuesta(
		Long id,
		String clienteNombre,
		String clienteContacto,
		String mensajes,
		MotivoSolicitud motivo,
		EstadoSolicitud estado,
		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime fechaCreacion) {
}
