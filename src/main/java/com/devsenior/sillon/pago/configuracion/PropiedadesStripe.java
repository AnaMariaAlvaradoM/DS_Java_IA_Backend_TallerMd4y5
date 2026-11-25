
package com.devsenior.sillon.pago.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sillon.stripe")
public record PropiedadesStripe(
        String claveSecreta,
        String secretoWebhook,
        String precioPro,
        String precioCreditos,
        String urlApi
) {
}
