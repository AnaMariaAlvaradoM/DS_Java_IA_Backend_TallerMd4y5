package com.devSenior.sillon.cita.dto;

import com.devSenior.sillon.ia.dto.FacturaIa;

public record RecordatorioRespuesta(
		Long citaId,
		String telefonoCliente,
		String mensaje,
		int creditosRestantes,
		FacturaIa factura) {
}
