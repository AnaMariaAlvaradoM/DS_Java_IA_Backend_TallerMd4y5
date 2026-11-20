package com.devSenior.sillon.cita.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class FormatoRecordatorioTest {

	@Test
	void formateaHoraDineroYFecha() {
		assertEquals("10:30 a. m.", FormatoRecordatorio.hora(LocalTime.of(10, 30)));
		assertEquals("7:05 p. m.", FormatoRecordatorio.hora(LocalTime.of(19, 5)));
		assertEquals("$35.000", FormatoRecordatorio.dinero(35000));
		String datos = FormatoRecordatorio.datos("Corte", 45, "Sara", LocalDate.of(2026, 12, 1), LocalTime.of(10, 30),
				35000, "Calle 1");
		assertTrue(datos.contains("Sara"));
		assertTrue(datos.contains("10:30 a. m."));
		assertTrue(datos.contains("$35.000"));
		assertTrue(datos.contains("Calle 1"));
	}

}
