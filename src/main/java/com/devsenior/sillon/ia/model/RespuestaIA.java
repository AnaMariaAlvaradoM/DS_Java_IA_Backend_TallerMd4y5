package com.devsenior.sillon.ia.model;

import java.math.BigDecimal;

/** Respuesta normalizada de cualquier proveedor: el texto y la factura (tokens, costo y tiempo). */
public record RespuestaIA(
        String texto,
        String proveedor,
        String modelo,
        int tokensEntrada,
        int tokensSalida,
        BigDecimal costoEstimadoUsd,
        long milisegundos
) {
}
