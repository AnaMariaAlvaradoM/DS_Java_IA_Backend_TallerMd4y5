package com.devsenior.sillon.compartido;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ConfiguracionReloj {

    @Bean
    public Clock reloj(@Value("${sillon.zona-horaria}")String zona){
        return Clock.system(ZoneId.of(zona));
    }
}
