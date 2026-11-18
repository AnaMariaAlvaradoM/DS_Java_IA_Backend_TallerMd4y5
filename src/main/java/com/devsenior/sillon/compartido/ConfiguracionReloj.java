package com.devsenior.sillon.compartido;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj único del sistema con la zona horaria del negocio. Toda regla que
 * dependa de "hoy" o "ahora" recibe este bean por constructor: Render corre
 * en UTC y la hora del servidor no es la del salón.
 */
@Configuration
public class ConfiguracionReloj {

    @Bean
    public Clock clock(@Value("${sillon.zona-horaria}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
