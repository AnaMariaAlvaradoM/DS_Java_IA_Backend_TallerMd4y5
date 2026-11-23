package com.devsenior.sillon.ia;

import java.util.Set;

public class ProveedorIAException extends RuntimeException {

    private static final Set<Integer> ERRORES_DE_CONFIGURACION = Set.of(400, 401, 403, 404);

    private final String proveedor;
    private final int codigoHttp;
    private final boolean errorDeConfiguracion;

    public ProveedorIAException(String proveedor, String motivo, boolean errorDeConfiguracion) {
        super(proveedor + ": " + motivo);
        this.proveedor = proveedor;
        this.codigoHttp = 0;
        this.errorDeConfiguracion = errorDeConfiguracion;
    }

    public ProveedorIAException(String proveedor, int codigoHttp, boolean errorDeConfiguracion, String mensaje,
            Throwable causa) {
        super(mensaje, causa);
        this.proveedor = proveedor;
        this.codigoHttp = codigoHttp;
        this.errorDeConfiguracion = errorDeConfiguracion;
    }

    public static ProveedorIAException desdeEstadoHttp(String proveedor, int estadoHttp) {
        boolean deConfiguracion = ERRORES_DE_CONFIGURACION.contains(estadoHttp);
        return new ProveedorIAException(proveedor, "respondió HTTP " + estadoHttp, deConfiguracion);
    }

    public String getProveedor() {
        return proveedor;
    }

    public int getCodigoHttp() {
        return codigoHttp;
    }

    public boolean isErrorDeConfiguracion() {
        return errorDeConfiguracion;
    }
}
