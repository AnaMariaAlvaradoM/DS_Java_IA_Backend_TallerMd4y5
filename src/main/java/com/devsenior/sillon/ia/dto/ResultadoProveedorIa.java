
package com.devsenior.sillon.ia.dto;

import java.util.Objects;

public record ResultadoProveedorIa(
        String contenido,
        FacturaIa factura) {

    public ResultadoProveedorIa {
        Objects.requireNonNull(
                contenido,
                "El contenido de IA no puede ser nulo");

        Objects.requireNonNull(
                factura,
                "La factura de IA no puede ser nula");
    }
}
