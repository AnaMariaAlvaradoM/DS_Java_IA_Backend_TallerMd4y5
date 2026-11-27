
package com.devsenior.sillon.ia.configuracion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class ConfiguracionJsonIa {

    @Bean
    public ObjectMapper objectMapperIa() {
        return new ObjectMapper();
    }
}
