package com.devsenior.sillon.pago;

public record PagoRespuesta(TipoCompra tipo, String sesionId, String urlPago) {
}
