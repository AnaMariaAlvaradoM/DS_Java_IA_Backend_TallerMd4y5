package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class FirmaInvalidaException extends ExcepcionSillon {
    public FirmaInvalidaException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, mensaje);
    }
}
