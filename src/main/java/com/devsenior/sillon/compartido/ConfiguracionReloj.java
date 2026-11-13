package com.devsenior.sillon.compartido;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionReloj {

    public static final ZoneId ZONA_DEL_NEGOCIO = ZoneId.of("America/Bogota");

    @Bean
    public Clock relojDelNegocio() {
        return Clock.system(ZONA_DEL_NEGOCIO);
    }
}
