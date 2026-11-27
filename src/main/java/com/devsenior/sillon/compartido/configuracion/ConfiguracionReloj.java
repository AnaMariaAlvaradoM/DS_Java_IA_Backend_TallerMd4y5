package com.devsenior.sillon.compartido.configuracion;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionReloj {

	private final String zonaHoraria;

	public ConfiguracionReloj(@Value("${sillon.zona-horaria}") String zonaHoraria) {
		this.zonaHoraria = zonaHoraria;
	}

	@Bean
	public Clock reloj() {
		return Clock.system(ZoneId.of(zonaHoraria));
	}

}
