package com.devSenior.sillon.salon.dto;

import java.time.LocalTime;

public record ConfiguracionRespuesta(
		LocalTime horaApertura,
		LocalTime horaCierre,
		boolean abreDomingos,
		int intervaloMinutos) {
}
