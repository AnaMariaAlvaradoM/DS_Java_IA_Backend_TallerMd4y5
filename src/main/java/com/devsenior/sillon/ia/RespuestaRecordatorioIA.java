
package com.devsenior.sillon.ia;

public record RespuestaRecordatorioIA(
        Long citaId,
        String telefonoCliente,
        String mensaje,
        int creditosRestantes,
        FacturaIA factura
) {
}
