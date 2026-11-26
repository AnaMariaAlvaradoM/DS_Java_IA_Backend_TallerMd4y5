package com.devsenior.sillon.compartido;

import org.springframework.http.HttpStatus;

/** 400: firma del webhook inválida. */
public class FirmaInvalidaException extends ApiException {

    public FirmaInvalidaException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, mensaje);
    }
}
