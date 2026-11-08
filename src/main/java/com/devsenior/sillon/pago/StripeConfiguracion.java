package com.devsenior.sillon.pago;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sillon.stripe")
public record StripeConfiguracion(String claveSecreta, String secretoWebhook,
        String precioPro, String precioCreditos, String urlApi) {
}
