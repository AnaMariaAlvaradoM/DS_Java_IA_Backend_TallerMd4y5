package com.devsenior.sillon.compartido;

import org.springframework.http.HttpStatus;

public class SolicitudInvalidaException extends ExcepcionApi {
    public SolicitudInvalidaException(String mensaje) { super(HttpStatus.BAD_REQUEST, mensaje); }
}
