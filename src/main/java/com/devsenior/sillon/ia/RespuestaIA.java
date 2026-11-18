package com.devsenior.sillon.ia;

import java.math.BigDecimal;

public record RespuestaIA(
        String texto,
        String proveedor,
        String modelo,
        int tokensEntrada,
        int tokensSalida,
        BigDecimal costoEstimadoUsd,
        long milisegundos) {

    public BigDecimal costoEstimado() {
        return costoEstimadoUsd;
    }

    public String getTexto() {
        return texto;
    }

    public String getProveedor() {
        return proveedor;
    }

    public String getModelo() {
        return modelo;
    }

    public int getTokensEntrada() {
        return tokensEntrada;
    }

    public int getTokensSalida() {
        return tokensSalida;
    }

    public BigDecimal getCostoEstimadoUsd() {
        return costoEstimadoUsd;
    }

    public long getMilisegundos() {
        return milisegundos;
    }
}
