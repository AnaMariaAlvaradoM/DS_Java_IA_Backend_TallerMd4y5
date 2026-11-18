package com.devsenior.sillon.cita.dto;

import com.devsenior.sillon.ia.FacturaIA;
import com.devsenior.sillon.ia.RespuestaIA;

public record RecordatorioResponse(
        Long citaId,
        String telefono,
        String mensaje,
        int creditosRestantes,
        RespuestaIA factura // Mantén el nombre "factura" en la llave si la guía lo pide, aunque el tipo sea RespuestaIA
) {}
