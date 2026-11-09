package com.devsenior.sillon.ia;

import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sillon.ia")
public record IaConfiguracion(Proveedor openai, Proveedor claude, Duration timeout) {
    public record Proveedor(String url, String llave, String modelo,
                            BigDecimal costoEntradaMillon, BigDecimal costoSalidaMillon) { }
}
