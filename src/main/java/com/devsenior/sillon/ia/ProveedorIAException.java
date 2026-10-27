package com.devsenior.sillon.ia;

import java.util.Set;

public class ProveedorIAException extends RuntimeException {

    private static final Set<Integer> ERRORES_DE_CONFIGURACION = Set.of(400, 401, 403, 404);

    private final boolean errorDeConfiguracion;

    public ProveedorIAException(String proveedor, String motivo, boolean errorDeConfiguracion) {
        super(proveedor + ": " + motivo);
        this.errorDeConfiguracion = errorDeConfiguracion;
    }

    public static ProveedorIAException desdeEstadoHttp(String proveedor, int estadoHttp) {
        boolean deConfiguracion = ERRORES_DE_CONFIGURACION.contains(estadoHttp);
        return new ProveedorIAException(proveedor, "respondió HTTP " + estadoHttp, deConfiguracion);
    }

    public boolean isErrorDeConfiguracion() {
        return errorDeConfiguracion;
    }
}
