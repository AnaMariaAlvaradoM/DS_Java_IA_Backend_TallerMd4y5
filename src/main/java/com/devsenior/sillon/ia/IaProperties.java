package com.devsenior.sillon.ia;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sillon.ia")
public record IaProperties(
        int timeoutSegundos,
        int maxTokens,
        Proveedor openai,
        Proveedor claude,
        Gemini gemini
) {

    public record Proveedor(
            String url,
            String modelo,
            String llave,
            BigDecimal precioEntradaMillon,
            BigDecimal precioSalidaMillon
    ) {

        private static final BigDecimal UN_MILLON = BigDecimal.valueOf(1_000_000);

        public BigDecimal calcularCosto(int tokensEntrada, int tokensSalida) {
            BigDecimal entrada = precioEntradaMillon.multiply(BigDecimal.valueOf(tokensEntrada));
            BigDecimal salida = precioSalidaMillon.multiply(BigDecimal.valueOf(tokensSalida));
            return entrada.add(salida).divide(UN_MILLON, 6, RoundingMode.HALF_UP);
        }
    }

    public record Gemini(String llave) {
    }
}
