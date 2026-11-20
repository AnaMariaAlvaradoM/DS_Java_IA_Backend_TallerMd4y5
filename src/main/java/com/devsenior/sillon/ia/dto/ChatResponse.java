package com.devsenior.sillon.ia.dto;

public record ChatResponse(
        String respuesta,
        boolean escalado,
        Long solicitudId,
        FacturaIa factura) {
}