package com.devsenior.sillon.pago.dto;

import com.devsenior.sillon.pago.TipoCompra;

public record SesionPagoResponse(TipoCompra tipo, String sesionId, String urlPago) {
}
