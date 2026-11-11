package com.devsenior.sillon.ia;

public class RespuestaIAInvalidaException extends RuntimeException {

    public RespuestaIAInvalidaException(String proveedor, String textoRecibido) {
        super("La IA (" + proveedor + ") respondió un valor no válido: " + textoRecibido);
    }
}
