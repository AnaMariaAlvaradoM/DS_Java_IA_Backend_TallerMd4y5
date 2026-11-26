
package com.devsenior.sillon.ia.dto;

public record RespuestaChatIa(
        String respuesta,
        boolean escalado,
        Long solicitudId,
        FacturaIa factura) {
}
