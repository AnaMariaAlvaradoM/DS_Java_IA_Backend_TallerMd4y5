package com.devsenior.sillon.ia.config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración tipada de la IA (prefijo sillon.ia). La URL de cada proveedor es configurable:
 * el calificador reemplaza sillon.ia.openai.url (SILLON_IA_OPENAI_URL) por su IA simulada.
 */
@ConfigurationProperties(prefix = "sillon.ia")
public record IaProperties(int timeoutSegundos, Proveedor openai, Proveedor claude) {

    public record Proveedor(
            String url,
            String llave,
            String modelo,
            BigDecimal precioEntradaMillon,
            BigDecimal precioSalidaMillon
    ) {

        /** costo = (tokensEntrada × precioEntrada + tokensSalida × precioSalida) ÷ 1.000.000 */
        public BigDecimal calcularCosto(int tokensEntrada, int tokensSalida) {
            BigDecimal entrada = precioEntradaMillon.multiply(BigDecimal.valueOf(tokensEntrada));
            BigDecimal salida = precioSalidaMillon.multiply(BigDecimal.valueOf(tokensSalida));
            return entrada.add(salida).divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);
        }

        public boolean tieneLlave() {
            return llave != null && !llave.isBlank() && !"sin-configurar".equals(llave);
        }

        public String urlSinBarraFinal() {
            return url == null ? "" : url.replaceAll("/+$", "");
        }
    }
}
