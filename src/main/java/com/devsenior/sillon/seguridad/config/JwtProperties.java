package com.devsenior.sillon.seguridad.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** sillon.jwt.secreto (variable JWT_SECRETO) y sillon.jwt.expiracion-minutos. */
@ConfigurationProperties(prefix = "sillon.jwt")
public record JwtProperties(String secreto, long expiracionMinutos) {
}
