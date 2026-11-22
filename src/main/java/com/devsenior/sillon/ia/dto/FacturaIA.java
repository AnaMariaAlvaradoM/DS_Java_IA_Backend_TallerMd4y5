package com.devsenior.sillon.ia.dto;

import java.math.BigDecimal;

import com.devsenior.sillon.ia.RespuestaIA;

public record FacturaIA(
        String proveedor,
        String modelo,
        int tokensEntrada,
        int tokensSalida,
        BigDecimal costoEstimadoUsd,
        long milisegundos) {

    public static FacturaIA de(RespuestaIA respuesta) {
        return new FacturaIA(
                respuesta.proveedor(),
                respuesta.modelo(),
                respuesta.tokensEntrada(),
                respuesta.tokensSalida(),
                respuesta.costoEstimadoUsd(),
                respuesta.milisegundos());
    }
}
