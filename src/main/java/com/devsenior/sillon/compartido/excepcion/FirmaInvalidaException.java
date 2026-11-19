package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

/** 400: la firma del webhook de Stripe no es válida. */
public class FirmaInvalidaException extends ExcepcionApi {

    public FirmaInvalidaException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, mensaje);
    }
}
