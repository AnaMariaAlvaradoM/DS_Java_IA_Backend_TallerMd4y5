package com.devsenior.sillon.compartido;

public class RespuestaIAInvalidaException extends RuntimeException {

    public RespuestaIAInvalidaException(String mensaje) {
        super(mensaje);
    }

    public RespuestaIAInvalidaException(String proveedor, String textoRecibido) {
        super("La IA (" + proveedor + ") respondió un valor no válido: " + textoRecibido);
    }
}
