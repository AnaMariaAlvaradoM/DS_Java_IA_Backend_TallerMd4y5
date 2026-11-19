package com.devsenior.sillon.pago.dto;

import com.devsenior.sillon.pago.model.TipoCompra;

public record PagoResponse(TipoCompra tipo, String sesionId, String urlPago) {
}
