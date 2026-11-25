package com.devsenior.sillon.compartido;

/** 404: el dato no existe o pertenece a otro salon (asi no se revela que existe). */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
