package com.devSenior.sillon.cita.servicio;

import java.time.LocalDateTime;

public record DatosRecordatorio(
		Long citaId,
		Long salonId,
		String salon,
		String cliente,
		String telefonoCliente,
		String datos,
		LocalDateTime inicio) {
}
