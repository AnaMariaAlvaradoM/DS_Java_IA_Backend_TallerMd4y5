package com.devsenior.sillon.pago.dto;

import com.devsenior.sillon.pago.TipoCompra;

public record SesionPagoRespuesta(
        TipoCompra tipo,
        String sesionId,
        String urlPago
) {
}
