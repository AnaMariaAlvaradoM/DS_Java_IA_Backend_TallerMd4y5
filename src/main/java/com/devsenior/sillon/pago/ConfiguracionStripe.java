package com.devsenior.sillon.pago;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sillon.stripe")
public record ConfiguracionStripe(String claveSecreta, String precioPro, String precioCreditos, String urlApi) {

    @Override
    public String toString() {
        return "ConfiguracionStripe[credenciales protegidas]";
    }
}
