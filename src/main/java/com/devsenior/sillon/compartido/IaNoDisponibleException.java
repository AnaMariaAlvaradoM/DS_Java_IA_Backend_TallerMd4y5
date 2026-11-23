package com.devsenior.sillon.compartido;

public class IaNoDisponibleException extends RuntimeException {

    public IaNoDisponibleException() {
        super("Ningún proveedor de IA está disponible en este momento");
    }
}
