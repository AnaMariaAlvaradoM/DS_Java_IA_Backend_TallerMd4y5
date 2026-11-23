
package com.devsenior.sillon.ia;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class CalculadoraCostosIA {

    private static final BigDecimal UN_MILLON =
            BigDecimal.valueOf(1_000_000);

    public double calcular(
            int tokensEntrada,
            int tokensSalida,
            double precioEntradaPorMillon,
            double precioSalidaPorMillon
    ) {

        if (tokensEntrada < 0
                || tokensSalida < 0
                || precioEntradaPorMillon < 0
                || precioSalidaPorMillon < 0
                || !Double.isFinite(precioEntradaPorMillon)
                || !Double.isFinite(precioSalidaPorMillon)) {

            throw new IllegalArgumentException(
                    "Los tokens y las tarifas deben ser valores válidos no negativos"
            );
        }

        BigDecimal costoEntrada =
                BigDecimal.valueOf(tokensEntrada)
                        .multiply(
                                BigDecimal.valueOf(
                                        precioEntradaPorMillon
                                )
                        );

        BigDecimal costoSalida =
                BigDecimal.valueOf(tokensSalida)
                        .multiply(
                                BigDecimal.valueOf(
                                        precioSalidaPorMillon
                                )
                        );

        return costoEntrada
                .add(costoSalida)
                .divide(
                        UN_MILLON,
                        8,
                        RoundingMode.HALF_UP
                )
                .doubleValue();
    }
}
