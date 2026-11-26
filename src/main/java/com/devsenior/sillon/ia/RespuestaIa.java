package com.devsenior.sillon.ia;

/** Lo que devolvió un proveedor: el texto y el uso de tokens. */
public record RespuestaIa(String texto, String proveedor, String modelo, int tokensEntrada, int tokensSalida,
        long milisegundos, double costoEstimadoUsd) {

    public Factura factura() {
        return new Factura(proveedor, modelo, tokensEntrada, tokensSalida, costoEstimadoUsd, milisegundos);
    }
}
