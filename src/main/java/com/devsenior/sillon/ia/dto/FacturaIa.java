package com.devsenior.sillon.ia.dto;

public record FacturaIa(
        String proveedor,
        String modelo,
        int tokensEntrada,
        int tokensSalida,
        double costoEstimadoUsd,
        long milisegundos) {
}