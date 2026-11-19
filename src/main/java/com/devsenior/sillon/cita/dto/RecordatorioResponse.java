package com.devsenior.sillon.cita.dto;

import com.devsenior.sillon.ia.dto.FacturaIA;

/** Borrador listo para que una persona lo revise y lo envíe. Sillón no lo envía. */
public record RecordatorioResponse(
        Long citaId,
        String telefonoCliente,
        String mensaje,
        int creditosRestantes,
        FacturaIA factura
) {
}
