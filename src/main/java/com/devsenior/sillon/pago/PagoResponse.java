package com.devsenior.sillon.pago;

public record PagoResponse(TipoCompra tipo, String sesionId, String urlPago) { }
