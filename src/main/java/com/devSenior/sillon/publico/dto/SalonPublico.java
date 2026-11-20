package com.devSenior.sillon.publico.dto;

import java.util.List;

public record SalonPublico(
		String nombre,
		String slug,
		String ciudad,
		String direccion,
		String telefono,
		String horario,
		List<ServicioPublico> servicios,
		List<EstilistaPublico> estilistas) {
}
