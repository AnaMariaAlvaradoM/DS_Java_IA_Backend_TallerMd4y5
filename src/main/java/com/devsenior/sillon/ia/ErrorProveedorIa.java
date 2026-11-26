package com.devsenior.sillon.ia;

/**
 * Falla de un proveedor. deConfiguracion = true cuando es un problema nuestro (llave, URL, modelo):
 * se registra como ERROR. Si es temporal (límite 429, caída 5xx, tiempo agotado) se registra como WARN.
 */
public class ErrorProveedorIa extends RuntimeException {

    private final boolean deConfiguracion;

    public ErrorProveedorIa(String mensaje, boolean deConfiguracion) {
        super(mensaje);
        this.deConfiguracion = deConfiguracion;
    }

    public boolean isDeConfiguracion() {
        return deConfiguracion;
    }

    public static boolean esErrorDeConfiguracion(int codigoHttp) {
        return codigoHttp == 400 || codigoHttp == 401 || codigoHttp == 403 || codigoHttp == 404;
    }
}
