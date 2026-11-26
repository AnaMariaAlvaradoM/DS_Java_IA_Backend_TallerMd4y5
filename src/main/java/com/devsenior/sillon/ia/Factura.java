package com.devsenior.sillon.ia;

/** Cuánto costó una respuesta de la IA (Clase 19). */
public record Factura(String proveedor, String modelo, int tokensEntrada, int tokensSalida, double costoEstimadoUsd,
        long milisegundos) {
}
