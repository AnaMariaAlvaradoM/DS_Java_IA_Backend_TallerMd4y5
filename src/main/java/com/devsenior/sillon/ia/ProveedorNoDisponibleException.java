package com.devsenior.sillon.ia;

public class ProveedorNoDisponibleException extends RuntimeException {

    private final boolean configuracion;

    public ProveedorNoDisponibleException(boolean configuracion) {
        super("Proveedor de IA no disponible");
        this.configuracion = configuracion;
    }

    public boolean esConfiguracion() {
        return configuracion;
    }
}
