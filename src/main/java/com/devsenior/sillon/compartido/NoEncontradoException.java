package com.devsenior.sillon.compartido;

public class NoEncontradoException extends RuntimeException {

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public NoEncontradoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
