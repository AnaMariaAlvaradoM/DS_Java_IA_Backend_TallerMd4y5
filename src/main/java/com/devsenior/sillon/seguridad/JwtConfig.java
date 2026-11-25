package com.devsenior.sillon.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion del JWT (prefijo sillon.jwt). El secreto viene de la variable de entorno JWT_SECRETO.
 * Para HS256 el secreto necesita al menos 32 caracteres (256 bits); con menos, la app no arranca.
 */
@ConfigurationProperties(prefix = "sillon.jwt")
public record JwtConfig(String secreto, long expiracionMinutos) {
}
