package com.devsenior.sillon.ia;

public class IaNoDisponibleException extends com.devsenior.sillon.compartido.excepcion.ProveedorNoDisponibleException {

    public IaNoDisponibleException() {
        super("Ningún proveedor de IA está disponible en este momento");
    }
}
