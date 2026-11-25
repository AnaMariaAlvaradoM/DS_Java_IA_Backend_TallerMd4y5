package com.devsenior.sillon.compartido;

/** 503: Ningun proveedor de IA esta disponible o configurado correctamente. */
public class IaNoDisponibleException extends RuntimeException {

    public IaNoDisponibleException() {
        super("Ningún proveedor de IA está disponible en este momento.");
    }

    public IaNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
