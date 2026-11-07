package com.devsenior.sillon.pago.dto;

public record PagoResponse(
        String tipo,
        String sesionId,
        String urlPago
) {
}
