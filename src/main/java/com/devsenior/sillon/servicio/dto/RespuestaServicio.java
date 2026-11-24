package com.devsenior.sillon.servicio.dto;

public record RespuestaServicio(

		Long id,
		String nombre,
		String descripcion,
		int duracionMinutos,
		int precio,
		boolean activo

) {
}
