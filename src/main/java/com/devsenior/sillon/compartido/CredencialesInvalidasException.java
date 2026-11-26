package com.devsenior.sillon.compartido;

import org.springframework.http.HttpStatus;

/** 401: credenciales incorrectas. */
public class CredencialesInvalidasException extends ApiException {

    public CredencialesInvalidasException(String mensaje) {
        super(HttpStatus.UNAUTHORIZED, mensaje);
    }
}
