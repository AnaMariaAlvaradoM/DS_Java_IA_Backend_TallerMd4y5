package com.devsenior.sillon.compartido;

public class ParametroInvalidoException extends RuntimeException {

    public ParametroInvalidoException(String mensaje) {
        super(mensaje);
    }

    public ParametroInvalidoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
