package com.devsenior.sillon.salon.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class ConfiguracionSalonTest {

	@Test
	void predeterminadaUsaElHorarioInicialDelContrato() {
		ConfiguracionSalon configuracion = ConfiguracionSalon.predeterminada();

		assertEquals(LocalTime.of(8, 0), configuracion.getHoraApertura());
		assertEquals(LocalTime.of(19, 0), configuracion.getHoraCierre());
		assertFalse(configuracion.isAbreDomingos());
		assertEquals(30, configuracion.getIntervaloMinutos());
	}

}
