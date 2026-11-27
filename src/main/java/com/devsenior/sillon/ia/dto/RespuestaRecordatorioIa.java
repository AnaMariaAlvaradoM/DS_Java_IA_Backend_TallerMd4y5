
package com.devsenior.sillon.ia.dto;

public record RespuestaRecordatorioIa(
        Long citaId,
        String telefonoCliente,
        String mensaje,
        int creditosRestantes,
        FacturaIa factura) {
}
