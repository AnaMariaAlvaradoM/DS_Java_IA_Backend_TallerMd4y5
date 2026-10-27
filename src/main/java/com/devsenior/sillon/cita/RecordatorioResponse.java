package com.devsenior.sillon.cita;

import com.devsenior.sillon.ia.dto.FacturaIA;

public record RecordatorioResponse(Long citaId, String telefonoCliente, String mensaje,
        int creditosRestantes, FacturaIA factura) {
}
