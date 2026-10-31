package com.devsenior.sillon.cita;

import com.devsenior.sillon.ia.dto.FacturaIA;

public record RecordatorioRespuesta(
        Long citaId,
        String telefonoCliente,
        String mensaje,
        int creditosRestantes,
        FacturaIA factura
) {
}
