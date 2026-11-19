package com.devsenior.sillon.ia.excepcion;

import java.util.Set;

/**
 * Falla de un proveedor. No llega al cliente: AsistenteIA la usa para decidir el log y pasar al siguiente.
 * Configuración (ERROR): 400, 401, 403, 404, llave faltante. Temporal (WARN): 429, 5xx, red, tiempo agotado, vacío.
 */
public class ProveedorIAException extends RuntimeException {

    private static final Set<Integer> CODIGOS_DE_CONFIGURACION = Set.of(400, 401, 403, 404);

    private final String proveedor;
    private final boolean errorDeConfiguracion;

    public ProveedorIAException(String proveedor, String mensaje, boolean errorDeConfiguracion) {
        super(proveedor + ": " + mensaje);
        this.proveedor = proveedor;
        this.errorDeConfiguracion = errorDeConfiguracion;
    }

    public static ProveedorIAException desdeCodigoHttp(String proveedor, int codigo) {
        return new ProveedorIAException(proveedor, "respondió HTTP " + codigo, esDeConfiguracion(codigo));
    }

    public static boolean esDeConfiguracion(int codigoHttp) {
        return CODIGOS_DE_CONFIGURACION.contains(codigoHttp);
    }

    public String getProveedor() {
        return proveedor;
    }

    public boolean isErrorDeConfiguracion() {
        return errorDeConfiguracion;
    }
}
