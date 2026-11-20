package com.devsenior.sillon.pago.dto;

import com.devsenior.sillon.pago.modelo.TipoCompra;

public record SesionPagoResponse(
        TipoCompra tipo,
        String sesionId,
        String urlPago) {
}