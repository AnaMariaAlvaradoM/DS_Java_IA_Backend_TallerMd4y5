package com.devsenior.sillon.compartido.excepcion;

public class ProveedorIaNoDisponibleException extends RuntimeException {
    public ProveedorIaNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
