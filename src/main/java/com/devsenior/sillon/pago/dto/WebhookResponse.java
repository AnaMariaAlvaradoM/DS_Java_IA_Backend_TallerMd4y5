package com.devsenior.sillon.pago.dto;

/** procesado = false cuando el evento ya se había aplicado antes (idempotencia). */
public record WebhookResponse(boolean recibido, boolean procesado) {
}
