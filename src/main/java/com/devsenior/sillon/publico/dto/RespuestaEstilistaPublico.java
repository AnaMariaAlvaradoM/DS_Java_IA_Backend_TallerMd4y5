package com.devsenior.sillon.publico.dto;

import java.util.List;

public record RespuestaEstilistaPublico(

		Long id,
		String nombre,
		List<Long> servicioIds

) {
}
