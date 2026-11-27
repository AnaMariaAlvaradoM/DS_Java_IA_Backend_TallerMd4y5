package com.devsenior.sillon.publico.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public record RespuestaDisponibilidad(

		@JsonFormat(pattern = "yyyy-MM-dd")
		LocalDate fecha,

		String servicio,
		String estilista,
		int duracionMinutos,
		List<String> horariosLibres

) {
}
