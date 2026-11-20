package com.devsenior.sillon.compartido.excepcion;

/** 400: la firma del webhook falta o no es válida. */
public class FirmaInvalidaException extends RuntimeException {

    public FirmaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
