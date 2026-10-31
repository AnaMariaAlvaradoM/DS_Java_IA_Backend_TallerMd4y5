package com.devsenior.sillon.ia.dto;

import com.devsenior.sillon.ia.RespuestaIA;
import java.math.BigDecimal;

public record FacturaIA(
        String proveedor,
        String modelo,
        int tokensEntrada,
        int tokensSalida,
        BigDecimal costoEstimadoUsd,
        long milisegundos
) {

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