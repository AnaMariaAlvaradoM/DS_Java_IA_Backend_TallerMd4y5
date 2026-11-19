
package com.devsenior.sillon.pago;

public record RespuestaCheckout(
        TipoCompra tipo,
        String sesionId,
        String urlPago
) {
}
