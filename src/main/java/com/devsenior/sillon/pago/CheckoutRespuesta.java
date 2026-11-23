package com.devsenior.sillon.pago;

public record CheckoutRespuesta(
        TipoCompra tipo,
        String sesionId,
        String urlPago) {
}
