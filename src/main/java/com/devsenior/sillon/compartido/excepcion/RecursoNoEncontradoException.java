package com.devsenior.sillon.compartido.excepcion;

/** 404: el recurso no existe o pertenece a otro salón. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
