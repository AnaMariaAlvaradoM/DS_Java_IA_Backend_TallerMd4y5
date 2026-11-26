package com.devsenior.sillon.compartido;

import org.springframework.http.HttpStatus;

/** 503: ningún proveedor de IA está disponible. */
public class IaNoDisponibleException extends ApiException {

    public IaNoDisponibleException(String mensaje) {
        super(HttpStatus.SERVICE_UNAVAILABLE, mensaje);
    }
}
