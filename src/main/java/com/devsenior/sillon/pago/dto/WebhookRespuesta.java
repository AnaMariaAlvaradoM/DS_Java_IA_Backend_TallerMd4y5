package com.devsenior.sillon.pago.dto;

public record WebhookRespuesta(
        boolean recibido,
        boolean procesado
) {
}
