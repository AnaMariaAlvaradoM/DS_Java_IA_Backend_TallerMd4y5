package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

/**
 * Base de todas las excepciones propias de Sillón.
 * Cada subclase fija su código HTTP; el manejador global la convierte en el formato único de error.
 */
public abstract class ExcepcionApi extends RuntimeException {

    private final HttpStatus estado;

    protected ExcepcionApi(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }
}
