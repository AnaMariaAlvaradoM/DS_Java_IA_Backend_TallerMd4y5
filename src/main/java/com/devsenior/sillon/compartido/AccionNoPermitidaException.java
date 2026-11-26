package com.devsenior.sillon.compartido;

import org.springframework.http.HttpStatus;

/** 403: acción no permitida para este rol. */
public class AccionNoPermitidaException extends ApiException {

    public AccionNoPermitidaException(String mensaje) {
        super(HttpStatus.FORBIDDEN, mensaje);
    }
}
