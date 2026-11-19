package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

/** 401: credenciales incorrectas. */
public class CredencialesInvalidasException extends ExcepcionApi {

    public CredencialesInvalidasException(String mensaje) {
        super(HttpStatus.UNAUTHORIZED, mensaje);
    }
}
