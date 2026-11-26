package com.devsenior.sillon.cita;

import com.devsenior.sillon.ia.Factura;

public record RecordatorioRespuesta(Long citaId, String telefonoCliente, String mensaje, int creditosRestantes,
        Factura factura) {
}
