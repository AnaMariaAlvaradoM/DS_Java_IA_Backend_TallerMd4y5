package com.devsenior.sillon.compartido;

public class IaNoDisponibleException extends RuntimeException {
    public IaNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
