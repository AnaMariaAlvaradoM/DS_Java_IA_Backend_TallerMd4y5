package com.devsenior.sillon.ia;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración tipada con prefijo sillon.ia (nada de URLs ni llaves fijas en el código). */
@ConfigurationProperties(prefix = "sillon.ia")
public record IaPropiedades(Proveedor openai, Proveedor claude, Integer timeoutSegundos) {

    public record Proveedor(String url, String llave, String modelo) {
    }

    public int timeout() {
        return timeoutSegundos == null ? 20 : timeoutSegundos;
    }
}
