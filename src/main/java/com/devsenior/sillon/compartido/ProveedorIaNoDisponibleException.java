package com.devsenior.sillon.compartido;

import org.springframework.http.HttpStatus;

public class ProveedorIaNoDisponibleException extends ExcepcionApi {
    public ProveedorIaNoDisponibleException(String mensaje) { super(HttpStatus.SERVICE_UNAVAILABLE, mensaje); }
}
