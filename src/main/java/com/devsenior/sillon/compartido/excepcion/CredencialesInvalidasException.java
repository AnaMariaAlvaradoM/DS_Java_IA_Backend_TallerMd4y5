package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class CredencialesInvalidasException extends ExcepcionSillon {
    public CredencialesInvalidasException(String mensaje) {
        super(HttpStatus.UNAUTHORIZED, mensaje);
    }
}
