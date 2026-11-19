package com.devsenior.sillon.ia;

import java.math.BigDecimal;

public record FacturaIa(String proveedor, String modelo, long tokensEntrada, long tokensSalida,
        BigDecimal costoEstimadoUsd, long milisegundos) {
}
