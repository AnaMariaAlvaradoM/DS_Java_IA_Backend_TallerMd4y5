package com.devsenior.sillon.compartido.configuracion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class ConfiguracionRelojTest {

	@Test
	void relojUsaLaZonaAmericaBogota() {
		ConfiguracionReloj configuracion = new ConfiguracionReloj("America/Bogota");

		assertEquals(ZoneId.of("America/Bogota"), configuracion.reloj().getZone());
	}

}
