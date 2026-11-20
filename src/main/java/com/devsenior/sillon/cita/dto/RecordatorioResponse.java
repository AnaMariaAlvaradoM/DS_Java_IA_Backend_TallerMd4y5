package com.devsenior.sillon.cita.dto;

import com.devsenior.sillon.ia.dto.FacturaIa;

public record RecordatorioResponse(
        Long citaId,
        String telefonoCliente,
        String mensaje,
        int creditosRestantes,
        FacturaIa factura) {
}