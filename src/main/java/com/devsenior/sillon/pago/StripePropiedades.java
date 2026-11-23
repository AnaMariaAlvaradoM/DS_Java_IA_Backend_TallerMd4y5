package com.devsenior.sillon.pago;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stripe")
public record StripePropiedades(
        String claveSecreta,
        String secretoWebhook,
        String precioPro,
        String precioCreditos,
        String urlApi) {
}
