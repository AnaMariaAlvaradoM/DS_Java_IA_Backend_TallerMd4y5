
package com.devsenior.sillon.ia;

public record FacturaIA(
        String proveedor,
        String modelo,
        int tokensEntrada,
        int tokensSalida,
        double costoEstimadoUsd,
        long milisegundos
) {
}
