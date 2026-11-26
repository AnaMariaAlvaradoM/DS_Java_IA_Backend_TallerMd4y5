package com.devsenior.sillon.compartido;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj del negocio. Render corre en UTC: "hoy" y "ahora" siempre se calculan
 * con la zona de Colombia. En los tests se reemplaza por un Clock.fixed.
 */
@Configuration
public class RelojConfig {

    @Bean
    public Clock clock(@Value("${sillon.zona-horaria:America/Bogota}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}
