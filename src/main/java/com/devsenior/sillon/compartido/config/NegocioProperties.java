package com.devsenior.sillon.compartido.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Números del negocio leídos de application.properties (prefijo sillon.negocio). */
@ConfigurationProperties(prefix = "sillon.negocio")
public record NegocioProperties(
        int creditosRegistro,
        int creditosPro,
        int creditosPaquete,
        int limiteEstilistasGratis
) {
}
