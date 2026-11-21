package com.devsenior.sillon.compartido;

/** 502: Stripe o la IA respondieron algo que no se pudo usar. */
public class PasarelaException extends RuntimeException {

    public PasarelaException(String mensaje) {
        super(mensaje);
    }
}
