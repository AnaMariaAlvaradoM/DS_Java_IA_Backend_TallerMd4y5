package com.devsenior.sillon.cita.dto;

import com.devsenior.sillon.ia.dto.FacturaIaDTO;

public record RecordatorioRespuesta(
        Long citaId,
        String telefonoCliente,
        String mensaje,
        int creditosRestantes,
        FacturaIaDTO factura
) {
}
