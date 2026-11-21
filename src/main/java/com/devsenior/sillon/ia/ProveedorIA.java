
package com.devsenior.sillon.ia;

public interface ProveedorIA {

    String nombre();

    Resultado generar(String instrucciones, String mensaje);

    record Resultado(
            String contenido,
            FacturaIA factura
    ) {
    }
}
