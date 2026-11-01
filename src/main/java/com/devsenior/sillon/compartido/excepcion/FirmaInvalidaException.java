package com.devsenior.sillon.compartido.excepcion;

public class FirmaInvalidaException extends RuntimeException {
    public FirmaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
