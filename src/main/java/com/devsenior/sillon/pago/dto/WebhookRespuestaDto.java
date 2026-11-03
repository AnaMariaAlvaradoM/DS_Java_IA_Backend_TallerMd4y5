package com.devsenior.sillon.pago.dto;

public record WebhookRespuestaDto(
        boolean recibido,
        boolean procesado
) {}
