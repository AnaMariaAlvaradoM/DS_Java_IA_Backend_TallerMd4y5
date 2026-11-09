package com.devsenior.sillon.ia;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record FacturaIa(String proveedor, String modelo, Integer tokensEntrada,
                        Integer tokensSalida, BigDecimal costoEstimadoUsd, long milisegundos) {
    public static FacturaIa crear(String proveedor, String modelo, Integer entrada, Integer salida,
                                  long milisegundos, IaConfiguracion.Proveedor configuracion) {
        BigDecimal costo = null;
        if (entrada != null && salida != null && configuracion.costoEntradaMillon() != null
                && configuracion.costoSalidaMillon() != null) {
            costo = configuracion.costoEntradaMillon().multiply(BigDecimal.valueOf(entrada))
                    .add(configuracion.costoSalidaMillon().multiply(BigDecimal.valueOf(salida)))
                    .divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);
        }
        return new FacturaIa(proveedor, modelo, entrada, salida, costo, milisegundos);
    }
}
