package com.devsenior.sillon.compartido.excepcion;

/** 403: el rol no tiene permiso para esta acción. */
public class AccionNoPermitidaException extends RuntimeException {

    public AccionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
