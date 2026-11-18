package com.devsenior.sillon.compartido.excepcion;

/** 502: Stripe o la IA respondieron algo que no se pudo usar. */
public class ErrorDePasarelaException extends RuntimeException {

    public ErrorDePasarelaException(String mensaje) {
        super(mensaje);
    }
}
