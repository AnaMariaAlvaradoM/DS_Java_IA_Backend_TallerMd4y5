
package com.devsenior.sillon.ia;

public record RespuestaChatIA(
        String respuesta,
        boolean escalado,
        Long solicitudId,
        FacturaIA factura
) {
}
