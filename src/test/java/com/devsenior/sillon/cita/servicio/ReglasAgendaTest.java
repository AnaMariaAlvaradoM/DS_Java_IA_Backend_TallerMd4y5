package com.devsenior.sillon.cita.servicio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devsenior.sillon.cita.modelo.Cita;
import com.devsenior.sillon.cita.modelo.EstadoCita;
import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.salon.modelo.ConfiguracionSalon;

class ReglasAgendaTest {

	private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");
	// Miércoles 2026-11-25 10:00:00 en America/Bogota
	private static final Instant INSTANTE_ACTUAL = Instant.parse("2026-11-25T15:00:00Z");

	private ReglasAgenda reglasAgenda;
	private ConfiguracionSalon configuracion;

	@BeforeEach
	void preparar() {
		Clock reloj = Clock.fixed(INSTANTE_ACTUAL, ZONA_BOGOTA);
		reglasAgenda = new ReglasAgenda(reloj);
		configuracion = ConfiguracionSalon.predeterminada(); // 08:00-19:00, domingos false, intervalo 30
	}

	@Test
	void calcularFinSumaDuracionDelServicio() {
		LocalDateTime inicio = LocalDateTime.of(2026, 11, 26, 10, 0);

		assertEquals(LocalDateTime.of(2026, 11, 26, 10, 45), reglasAgenda.calcularFin(inicio, 45));
	}

	@Test
	void rechazaCitaEnElPasadoConConflicto() {
		LocalDateTime pasado = LocalDateTime.of(2026, 11, 25, 9, 59);

		assertThrows(ExcepcionConflicto.class, () -> reglasAgenda.validarNoEsEnElPasado(pasado));
	}

	@Test
	void rechazaDomingoCuandoSalonNoAbreDomingos() {
		// 2026-11-29 es domingo
		LocalDateTime inicio = LocalDateTime.of(2026, 11, 29, 10, 0);
		LocalDateTime fin = LocalDateTime.of(2026, 11, 29, 10, 30);

		assertThrows(
				ExcepcionConflicto.class,
				() -> reglasAgenda.validarHorarioSalon(inicio, fin, configuracion));
	}

	@Test
	void permiteDomingoCuandoSalonAbreDomingos() {
		configuracion.setAbreDomingos(true);
		LocalDateTime inicio = LocalDateTime.of(2026, 11, 29, 10, 0);
		LocalDateTime fin = LocalDateTime.of(2026, 11, 29, 10, 30);

		assertDoesNotThrow(() -> reglasAgenda.validarHorarioSalon(inicio, fin, configuracion));
	}

	@Test
	void rechazaAntesDeAperturaOAlSuperarCierreConDuracionCompleta() {
		LocalDateTime muyTemprano = LocalDateTime.of(2026, 11, 26, 7, 45);
		assertThrows(
				ExcepcionConflicto.class,
				() -> reglasAgenda.validarHorarioSalon(muyTemprano, muyTemprano.plusMinutes(30), configuracion));

		LocalDateTime excedeCierre = LocalDateTime.of(2026, 11, 26, 18, 45);
		assertThrows(
				ExcepcionConflicto.class,
				() -> reglasAgenda.validarHorarioSalon(excedeCierre, excedeCierre.plusMinutes(30), configuracion));

		LocalDateTime cruzaMedianoche = LocalDateTime.of(2026, 11, 26, 23, 0);
		assertThrows(
				ExcepcionConflicto.class,
				() -> reglasAgenda.validarHorarioSalon(cruzaMedianoche, cruzaMedianoche.plusMinutes(120), configuracion));
	}

	@Test
	void permiteCitaExactamenteDeAperturaYHastaElCierre() {
		LocalDateTime enApertura = LocalDateTime.of(2026, 11, 26, 8, 0);
		assertDoesNotThrow(
				() -> reglasAgenda.validarHorarioSalon(enApertura, enApertura.plusMinutes(30), configuracion));

		LocalDateTime justoHastaCierre = LocalDateTime.of(2026, 11, 26, 18, 30);
		assertDoesNotThrow(
				() -> reglasAgenda.validarHorarioSalon(justoHastaCierre, LocalDateTime.of(2026, 11, 26, 19, 0), configuracion));
	}

	@Test
	void detectaCrucesPeroPermiteIntervalosContiguos() {
		LocalDateTime inicioExistente = LocalDateTime.of(2026, 11, 26, 10, 0);
		LocalDateTime finExistente = LocalDateTime.of(2026, 11, 26, 10, 30);
		Cita confirmada = cita(inicioExistente, finExistente, EstadoCita.CONFIRMADA);

		// Contiguo posterior (10:30 - 11:00) y anterior (09:30 - 10:00)
		assertFalse(reglasAgenda.seCruzan(
				LocalDateTime.of(2026, 11, 26, 10, 30),
				LocalDateTime.of(2026, 11, 26, 11, 0),
				inicioExistente,
				finExistente));
		assertDoesNotThrow(() -> reglasAgenda.validarSinCruces(
				LocalDateTime.of(2026, 11, 26, 9, 30),
				LocalDateTime.of(2026, 11, 26, 10, 0),
				List.of(confirmada)));
		assertDoesNotThrow(() -> reglasAgenda.validarSinCruces(
				LocalDateTime.of(2026, 11, 26, 10, 30),
				LocalDateTime.of(2026, 11, 26, 11, 0),
				List.of(confirmada)));

		// Solapamiento parcial, contenido y envolvente
		assertThrows(ExcepcionConflicto.class, () -> reglasAgenda.validarSinCruces(
				LocalDateTime.of(2026, 11, 26, 10, 15),
				LocalDateTime.of(2026, 11, 26, 10, 45),
				List.of(confirmada)));
		assertThrows(ExcepcionConflicto.class, () -> reglasAgenda.validarSinCruces(
				LocalDateTime.of(2026, 11, 26, 9, 45),
				LocalDateTime.of(2026, 11, 26, 10, 45),
				List.of(confirmada)));
	}

	@Test
	void citasTerminalesNoBloqueanAgendaMientrasPendienteSiBloquea() {
		Cita cancelada = cita(
				LocalDateTime.of(2026, 11, 26, 10, 0),
				LocalDateTime.of(2026, 11, 26, 10, 30),
				EstadoCita.CANCELADA);
		Cita completada = cita(
				LocalDateTime.of(2026, 11, 26, 10, 0),
				LocalDateTime.of(2026, 11, 26, 10, 30),
				EstadoCita.COMPLETADA);
		Cita noAsistio = cita(
				LocalDateTime.of(2026, 11, 26, 10, 0),
				LocalDateTime.of(2026, 11, 26, 10, 30),
				EstadoCita.NO_ASISTIO);

		assertDoesNotThrow(() -> reglasAgenda.validarSinCruces(
				LocalDateTime.of(2026, 11, 26, 10, 0),
				LocalDateTime.of(2026, 11, 26, 10, 30),
				List.of(cancelada, completada, noAsistio)));

		Cita pendiente = cita(
				LocalDateTime.of(2026, 11, 26, 10, 0),
				LocalDateTime.of(2026, 11, 26, 10, 30),
				EstadoCita.PENDIENTE);

		assertThrows(ExcepcionConflicto.class, () -> reglasAgenda.validarSinCruces(
				LocalDateTime.of(2026, 11, 26, 10, 0),
				LocalDateTime.of(2026, 11, 26, 10, 30),
				List.of(pendiente)));
	}

	@Test
	void calcularHorariosLibresRespetaIntervaloDuracionCompletaPasadoYCitasActivas() {
		configuracion.setHoraApertura(LocalTime.of(9, 0));
		configuracion.setHoraCierre(LocalTime.of(12, 0));
		configuracion.setIntervaloMinutos(30);

		// Hoy es 2026-11-25 10:00 -> 09:00 y 09:30 ya pasaron
		// Cita PENDIENTE 10:30-11:15 bloquea 10:00 (termina 10:45) y 10:30 (termina 11:15) y 11:00 (termina 11:45)
		// Cita CANCELADA 11:00-11:30 no bloquea
		// Servicio de 45 min: 11:30 termina 12:15 > 12:00 (no cabe)
		// Único slot libre esperado: 10:00? No, 10:00-10:45 cruza 10:30-11:15 -> entonces 11:15? No está en múltiplos de 30.
		// Probemos con cita CONFIRMADA 10:30-11:00:
		// 10:00-10:45 cruza 10:30-11:00 -> excluido
		// 10:30-11:15 cruza 10:30-11:00 -> excluido
		// 11:00-11:45 contiguo a 10:30-11:00 y <= 12:00 -> LIBRE
		// 11:30-12:15 excede cierre 12:00 -> excluido
		Cita confirmada = cita(
				LocalDateTime.of(2026, 11, 25, 10, 30),
				LocalDateTime.of(2026, 11, 25, 11, 0),
				EstadoCita.CONFIRMADA);
		Cita cancelada = cita(
				LocalDateTime.of(2026, 11, 25, 11, 0),
				LocalDateTime.of(2026, 11, 25, 11, 30),
				EstadoCita.CANCELADA);

		List<LocalTime> libres = reglasAgenda.calcularHorariosLibres(
				LocalDate.of(2026, 11, 25),
				45,
				configuracion,
				List.of(confirmada, cancelada));

		assertEquals(List.of(LocalTime.of(11, 0)), libres);
	}

	@Test
	void calcularHorariosLibresDevuelveVacioEnDomingoCerradoYFechaPasada() {
		assertTrue(reglasAgenda.calcularHorariosLibres(
				LocalDate.of(2026, 11, 29),
				30,
				configuracion,
				List.of()).isEmpty());

		assertTrue(reglasAgenda.calcularHorariosLibres(
				LocalDate.of(2026, 11, 24),
				30,
				configuracion,
				List.of()).isEmpty());
	}

	private Cita cita(LocalDateTime inicio, LocalDateTime fin, EstadoCita estado) {
		Cita cita = new Cita();
		cita.setInicio(inicio);
		cita.setFin(fin);
		cita.setEstado(estado);
		return cita;
	}

}
