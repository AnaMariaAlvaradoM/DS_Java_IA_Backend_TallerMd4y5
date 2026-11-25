package com.devsenior.sillon.publico.dto;

public record RespuestaServicioPublico(

		Long id,
		String nombre,
		String descripcion,
		int duracionMinutos,
		int precio

) {
}
