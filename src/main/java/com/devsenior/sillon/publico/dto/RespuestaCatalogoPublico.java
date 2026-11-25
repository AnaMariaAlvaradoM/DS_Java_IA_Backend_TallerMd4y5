package com.devsenior.sillon.publico.dto;

import java.util.List;

public record RespuestaCatalogoPublico(

		String nombre,
		String slug,
		String ciudad,
		String direccion,
		String telefono,
		String horario,
		List<RespuestaServicioPublico> servicios,
		List<RespuestaEstilistaPublico> estilistas

) {
}
