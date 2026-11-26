package com.devsenior.sillon.compartido;

import org.springframework.http.HttpStatus;

/** 409: choca con una regla de negocio. */
public class ReglaNegocioException extends ApiException {

    public ReglaNegocioException(String mensaje) {
        super(HttpStatus.CONFLICT, mensaje);
    }
}
