package com.devsenior.sillon.cita.dto;

import com.devsenior.sillon.ia.dto.FacturaIA;

public record RecordatorioRespuestaDto(
        Long citaId,
        String telefonoCliente,
        String mensaje,
        int creditosRestantes,
        FacturaIA factura
) {}
