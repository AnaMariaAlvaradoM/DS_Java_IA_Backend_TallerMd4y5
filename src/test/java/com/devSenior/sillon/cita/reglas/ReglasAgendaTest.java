package com.devSenior.sillon.cita.reglas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devSenior.sillon.compartido.excepcion.ConflictoException;
import com.devSenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devSenior.sillon.salon.entidad.ConfiguracionSalon;

class ReglasAgendaTest {

	private static final ZoneId ZONA = ZoneId.of("America/Bogota");
	private ReglasAgenda reglas;
	private ConfiguracionSalon configuracion;

	@BeforeEach
	void preparar() {
		Clock reloj = Clock.fixed(LocalDateTime.of(2026, 11, 17, 10, 0).atZone(ZONA).toInstant(), ZONA);
		reglas = new ReglasAgenda(reloj);
		configuracion = ConfiguracionSalon.porDefecto();
	}

	@Test
	void aceptaHorarioValidoYDomingoAbierto() {
		reglas.validar(configuracion, LocalDateTime.of(2026, 11, 18, 10, 0), LocalDateTime.of(2026, 11, 18, 10, 45));
		configuracion.setAbreDomingos(true);
		reglas.validar(configuracion, LocalDateTime.of(2026, 11, 22, 10, 0), LocalDateTime.of(2026, 11, 22, 10, 45));
	}

	@Test
	void rechazaFueraDeAperturaCierreDomingoPasadoAnticipacionYAlineacion() {
		assertThrows(ConflictoException.class, () -> reglas.validar(configuracion,
				LocalDateTime.of(2026, 11, 18, 7, 30), LocalDateTime.of(2026, 11, 18, 8, 15)));
		assertThrows(ConflictoException.class, () -> reglas.validar(configuracion,
				LocalDateTime.of(2026, 11, 18, 18, 30), LocalDateTime.of(2026, 11, 18, 19, 15)));
		assertThrows(ConflictoException.class, () -> reglas.validar(configuracion,
				LocalDateTime.of(2026, 11, 22, 10, 0), LocalDateTime.of(2026, 11, 22, 10, 45)));
		assertThrows(FirmaInvalidaException.class, () -> reglas.validar(configuracion,
				LocalDateTime.of(2026, 11, 18, 10, 15), LocalDateTime.of(2026, 11, 18, 11, 0)));
		assertThrows(ConflictoException.class, () -> reglas.validar(configuracion,
				LocalDateTime.of(2026, 11, 17, 9, 0), LocalDateTime.of(2026, 11, 17, 9, 45)));
		assertThrows(FirmaInvalidaException.class, () -> reglas.validar(configuracion,
				LocalDateTime.of(2027, 6, 1, 10, 0), LocalDateTime.of(2027, 6, 1, 10, 45)));
	}

	@Test
	void detectaSolapamientoYAceptaCitaConsecutiva() {
		LocalDateTime inicio = LocalDateTime.of(2026, 11, 18, 10, 0);
		LocalDateTime fin = LocalDateTime.of(2026, 11, 18, 10, 45);
		assertTrue(reglas.seCruzan(inicio, fin, LocalDateTime.of(2026, 11, 18, 10, 30),
				LocalDateTime.of(2026, 11, 18, 11, 15)));
		assertFalse(reglas.seCruzan(inicio, fin, fin, LocalDateTime.of(2026, 11, 18, 11, 30)));
	}

	@Test
	void diaCerradoDevuelveDisponibilidadVacia() {
		List<LocalTime> libres = reglas.horariosLibres(configuracion, LocalDate.of(2026, 11, 22), 45, List.of());
		assertEquals(List.of(), libres);
	}

}
