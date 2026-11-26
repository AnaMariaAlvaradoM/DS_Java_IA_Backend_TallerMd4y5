package com.devsenior.sillon.compartido;

import org.springframework.http.HttpStatus;

/** 404: no existe o pertenece a otro salón. */
public class NoEncontradoException extends ApiException {

    public NoEncontradoException(String mensaje) {
        super(HttpStatus.NOT_FOUND, mensaje);
    }
}
