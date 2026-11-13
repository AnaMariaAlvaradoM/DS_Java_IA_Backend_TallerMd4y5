package com.devsenior.sillon.compartido;

public class PasarelaException extends RuntimeException {

    public PasarelaException(String mensaje) {
        super(mensaje);
    }

    public PasarelaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
