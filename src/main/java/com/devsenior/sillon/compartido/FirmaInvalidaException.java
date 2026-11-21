package com.devsenior.sillon.compartido;

/** 400: la firma del webhook de Stripe falta o es falsa. */
public class FirmaInvalidaException extends RuntimeException {

    public FirmaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
