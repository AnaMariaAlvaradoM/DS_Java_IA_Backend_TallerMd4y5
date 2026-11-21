package com.devsenior.sillon.compartido;

/** 403: el rol no tiene permiso, o un estilista intenta una transicion que no le corresponde. */
public class AccionNoPermitidaException extends RuntimeException {

    public AccionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
