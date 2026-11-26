package com.devsenior.sillon.compartido;

import org.springframework.http.HttpStatus;

/** Base de las excepciones propias: cada una sabe qué código HTTP le corresponde. */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus estado;

    protected ApiException(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }
}
