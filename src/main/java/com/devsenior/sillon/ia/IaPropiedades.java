package com.devsenior.sillon.ia;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sillon.ia")
public record IaPropiedades(
        int timeoutSegundos,
        int maxTokens,
        ProveedorConfig openai,
        ProveedorConfig claude,
        GeminiConfig gemini) {

    public record ProveedorConfig(
            String url,
            String modelo,
            String llave,
            BigDecimal precioEntradaMillon,
            BigDecimal precioSalidaMillon) {

        private static final BigDecimal UN_MILLON = BigDecimal.valueOf(1_000_000);

        public BigDecimal calcularCosto(int tokensEntrada, int tokensSalida) {
            BigDecimal pe = precioEntradaMillon != null ? precioEntradaMillon : BigDecimal.ZERO;
            BigDecimal ps = precioSalidaMillon != null ? precioSalidaMillon : BigDecimal.ZERO;
            BigDecimal entrada = pe.multiply(BigDecimal.valueOf(tokensEntrada));
            BigDecimal salida = ps.multiply(BigDecimal.valueOf(tokensSalida));
            return entrada.add(salida).divide(UN_MILLON, 6, RoundingMode.HALF_UP);
        }
    }

    public record GeminiConfig(String llave) {
    }
}
