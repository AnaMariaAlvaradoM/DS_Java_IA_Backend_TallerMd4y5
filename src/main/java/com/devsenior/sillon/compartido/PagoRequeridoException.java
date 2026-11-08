package com.devsenior.sillon.compartido;

public class PagoRequeridoException extends RuntimeException {

    public PagoRequeridoException(String mensaje) {
        super(mensaje);
    }

    public PagoRequeridoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
