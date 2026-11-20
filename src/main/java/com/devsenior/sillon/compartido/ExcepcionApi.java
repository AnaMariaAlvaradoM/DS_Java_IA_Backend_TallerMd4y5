package com.devsenior.sillon.compartido;

import org.springframework.http.HttpStatus;

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
