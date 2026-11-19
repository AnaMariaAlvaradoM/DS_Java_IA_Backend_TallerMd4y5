package com.devsenior.sillon.ia.dto;

import com.devsenior.sillon.ia.model.RespuestaIA;
import java.math.BigDecimal;

/** Lo que costó la respuesta. Viaja en la API sin exponer el texto crudo del modelo. */
public record FacturaIA(
        String proveedor,
        String modelo,
        int tokensEntrada,
        int tokensSalida,
        BigDecimal costoEstimadoUsd,
        long milisegundos
) {

    public static FacturaIA de(RespuestaIA respuesta) {
        return new FacturaIA(respuesta.proveedor(), respuesta.modelo(), respuesta.tokensEntrada(),
                respuesta.tokensSalida(), respuesta.costoEstimadoUsd(), respuesta.milisegundos());
    }
}
