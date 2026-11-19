package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

/** 403: el rol no tiene permiso para esta acción. */
public class AccionNoPermitidaException extends ExcepcionApi {

    public AccionNoPermitidaException(String mensaje) {
        super(HttpStatus.FORBIDDEN, mensaje);
    }
}
