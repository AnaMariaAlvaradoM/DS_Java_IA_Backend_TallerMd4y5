
package com.devsenior.sillon.pago.dto;

public record RespuestaWebhook(
        boolean recibido,
        boolean procesado
) {
}
