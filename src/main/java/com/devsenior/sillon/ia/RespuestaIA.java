package com.devsenior.sillon.ia;

import java.math.BigDecimal;

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
