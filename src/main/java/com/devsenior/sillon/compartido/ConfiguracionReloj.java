package com.devsenior.sillon.compartido;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "fechaAuditoria")
public class ConfiguracionReloj {
    @Bean
    public Clock reloj(@Value("${sillon.zona-horaria}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }

    @Bean
    public DateTimeProvider fechaAuditoria(Clock reloj) {
        return () -> Optional.of(java.time.LocalDateTime.now(reloj));
    }
}
