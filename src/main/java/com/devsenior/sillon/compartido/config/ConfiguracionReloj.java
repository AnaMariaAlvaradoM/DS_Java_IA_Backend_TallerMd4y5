package com.devsenior.sillon.compartido.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj del negocio. Render y el calificador corren en UTC: "hoy" y "ahora"
 * siempre se calculan con la zona de Colombia. En los tests se reemplaza por un reloj fijo.
 */
@Configuration
public class ConfiguracionReloj {

    @Bean
    public Clock clock(@Value("${sillon.zona-horaria}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
