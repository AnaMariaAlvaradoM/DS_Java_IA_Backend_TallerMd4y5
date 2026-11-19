package com.devsenior.sillon.pago.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de Stripe (prefijo sillon.stripe). La urlApi es configurable (STRIPE_URL_API)
 * porque el calificador apunta la API a su simulador de Stripe.
 */
@ConfigurationProperties(prefix = "sillon.stripe")
public record StripeProperties(
        String urlApi,
        String claveSecreta,
        String secretoWebhook,
        String precioPro,
        String precioCreditos
) {
}
