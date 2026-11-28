package com.devSenior.sillon.compartido.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RelojConfig {

	@Bean
	public Clock reloj(@Value("${sillon.zona-horaria}") String zonaHoraria) {
		return Clock.system(ZoneId.of(zonaHoraria));
	}

}
