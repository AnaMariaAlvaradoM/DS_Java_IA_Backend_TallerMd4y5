package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

/** 404: no existe o pertenece a otro salón. */
public class RecursoNoEncontradoException extends ExcepcionApi {

    public RecursoNoEncontradoException(String mensaje) {
        super(HttpStatus.NOT_FOUND, mensaje);
    }
}
