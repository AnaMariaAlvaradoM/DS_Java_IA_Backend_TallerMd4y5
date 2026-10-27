package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class RecursoNoEncontradoException extends ExcepcionSillon {
    public RecursoNoEncontradoException(String mensaje) {
        super(HttpStatus.NOT_FOUND, mensaje);
    }
}
