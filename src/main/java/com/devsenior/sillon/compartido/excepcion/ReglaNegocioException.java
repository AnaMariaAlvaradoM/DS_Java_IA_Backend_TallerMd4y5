package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

/** 409: la operación choca con una regla de negocio. */
public class ReglaNegocioException extends ExcepcionApi {

    public ReglaNegocioException(String mensaje) {
        super(HttpStatus.CONFLICT, mensaje);
    }
}
