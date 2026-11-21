package com.devsenior.sillon.compartido;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    @Value("${sillon.zona-horaria:America/Bogota}")
    private String zonaHoraria;

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
