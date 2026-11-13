package com.devsenior.sillon.compartido;

public class FirmaInvalidaException extends RuntimeException {

    public FirmaInvalidaException(String mensaje) {
        super(mensaje);
    }

    public FirmaInvalidaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
