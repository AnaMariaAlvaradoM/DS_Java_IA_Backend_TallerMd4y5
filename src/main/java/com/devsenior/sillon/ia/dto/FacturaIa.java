
package com.devsenior.sillon.ia.dto;

import java.math.BigDecimal;

public record FacturaIa(
        String proveedor,
        String modelo,
        int tokensEntrada,
        int tokensSalida,
        BigDecimal costoEstimadoUsd,
        long milisegundos) {
}
