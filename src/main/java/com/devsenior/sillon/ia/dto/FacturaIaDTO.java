package com.devsenior.sillon.ia.dto;

public record FacturaIaDTO(
        String proveedor,
        String modelo,
        int tokensEntrada,
        int tokensSalida,
        double costoEstimadoUsd,
        long milisegundos
) {
}
