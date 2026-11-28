package com.devSenior.sillon.salon.dto;

import java.time.LocalDateTime;

import com.devSenior.sillon.salon.entidad.Plan;
import com.fasterxml.jackson.annotation.JsonFormat;

public record SalonRespuesta(
		Long id,
		String nombre,
		String slug,
		String ciudad,
		String direccion,
		String telefono,
		Plan plan,
		int creditosIa,
		ConfiguracionRespuesta configuracion,
		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime fechaCreacion) {
}
