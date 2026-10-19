package com.devsenior.sillon.ia;

public class RespuestaIAInvalidaException extends com.devsenior.sillon.compartido.excepcion.PasarelaException {

    public RespuestaIAInvalidaException(String proveedor, String textoRecibido) {
        super("La IA respondió un valor no válido");
    }
}
