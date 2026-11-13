package com.devsenior.sillon.compartido;

public class AccionNoPermitidaException extends RuntimeException {

    public AccionNoPermitidaException(String mensaje) {
        super(mensaje);
    }

    public AccionNoPermitidaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
