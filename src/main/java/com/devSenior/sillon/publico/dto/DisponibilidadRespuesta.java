package com.devSenior.sillon.publico.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public record DisponibilidadRespuesta(
		@JsonFormat(pattern = "yyyy-MM-dd") LocalDate fecha,
		String servicio,
		String estilista,
		int duracionMinutos,
		@JsonFormat(pattern = "HH:mm:ss") List<LocalTime> horariosLibres) {
}
