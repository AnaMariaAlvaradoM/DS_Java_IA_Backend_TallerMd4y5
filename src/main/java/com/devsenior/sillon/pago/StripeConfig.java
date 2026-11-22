package com.devsenior.sillon.pago;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuración tipadas para la integración con Stripe.
 * Se leen del prefijo sillon.stripe en application.properties.
 */
@ConfigurationProperties(prefix = "sillon.stripe")
public record StripeConfig(
        String claveSecreta,
        String secretoWebhook,
        String precioPro,
        String precioCreditos,
        String urlApi) {
}
