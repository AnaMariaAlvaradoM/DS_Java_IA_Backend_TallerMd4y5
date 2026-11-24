package com.devsenior.sillon.compartido.configuracion;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "proveedorFechaAuditoria")
public class ConfiguracionAuditoriaJpa {

	@Bean
	public DateTimeProvider proveedorFechaAuditoria(Clock reloj) {
		return () -> Optional.of(LocalDateTime.now(reloj));
	}

}
