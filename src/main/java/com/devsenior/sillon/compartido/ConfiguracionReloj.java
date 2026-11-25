package com.devsenior.sillon.compartido;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj del sistema con la zona horaria del negocio (America/Bogota).
 * Todo lo que dependa de "hoy" o "ahora" recibe este Clock por constructor.
 * Render corre en UTC: sin esto, a las 8:00 p. m. en Bogota el servidor ya cree que es manana.
 * Con un reloj fijo, los tests dan siempre el mismo resultado.
 */
@Configuration
public class ConfiguracionReloj {

    @Bean
    public Clock reloj(@Value("${sillon.zona-horaria}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
