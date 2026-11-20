package com.devsenior.sillon.pago.dto;

public record WebhookResponse(
        boolean recibido,
        boolean procesado) {
}