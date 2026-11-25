package com.devsenior.sillon.compartido;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Los numeros del negocio, leidos de application.properties (prefijo sillon.negocio).
 * Asi ningun 10, 300 o 2 queda escrito dentro de las clases.
 */
@ConfigurationProperties(prefix = "sillon.negocio")
public record NegocioConfig(
        int creditosRegistro,
        int creditosPlanPro,
        int creditosPaquete,
        int limiteEstilistasGratis) {
}
