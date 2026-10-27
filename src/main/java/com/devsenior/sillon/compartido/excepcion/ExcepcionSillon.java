package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

public abstract class ExcepcionSillon extends RuntimeException {
    private final HttpStatus estado;

    protected ExcepcionSillon(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }
}
