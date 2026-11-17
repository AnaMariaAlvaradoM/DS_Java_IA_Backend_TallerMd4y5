package com.devSenior.sillon.servicio.dto;

public record ServicioRespuesta(
		Long id,
		String nombre,
		String descripcion,
		int duracionMinutos,
		int precio,
		boolean activo) {
}
