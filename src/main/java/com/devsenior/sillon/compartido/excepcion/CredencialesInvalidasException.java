package com.devsenior.sillon.compartido.excepcion;

/** 401: credenciales incorrectas o token inválido. */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
