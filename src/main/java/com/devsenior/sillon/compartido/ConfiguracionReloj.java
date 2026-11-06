
package com.devsenior.sillon.compartido;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionReloj {

    @Bean
    public Clock relojSistema() {
        return Clock.system(
                ZoneId.of("America/Bogota")
        );
    }
}
