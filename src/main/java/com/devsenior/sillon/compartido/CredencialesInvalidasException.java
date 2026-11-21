package com.devsenior.sillon.compartido;

/** 401: credenciales incorrectas. El mensaje es generico a proposito: no dice si fallo el correo o la contrasena. */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
