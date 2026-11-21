package com.devsenior.sillon.pago;

public record SesionPagoResponse(TipoCompra tipo, String sesionId, String urlPago) {
}
