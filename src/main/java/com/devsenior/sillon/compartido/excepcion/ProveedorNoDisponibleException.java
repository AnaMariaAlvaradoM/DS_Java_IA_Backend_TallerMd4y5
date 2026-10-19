package com.devsenior.sillon.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class ProveedorNoDisponibleException extends ExcepcionSillon {
    public ProveedorNoDisponibleException(String mensaje) {
        super(HttpStatus.SERVICE_UNAVAILABLE, mensaje);
    }
}
