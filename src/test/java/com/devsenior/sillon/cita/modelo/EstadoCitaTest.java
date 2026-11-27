package com.devsenior.sillon.cita.modelo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class EstadoCitaTest {

	@Test
	void soloPendienteYConfirmadaOcupanAgenda() {
		assertTrue(EstadoCita.PENDIENTE.ocupaAgenda());
		assertTrue(EstadoCita.CONFIRMADA.ocupaAgenda());
		assertFalse(EstadoCita.COMPLETADA.ocupaAgenda());
		assertFalse(EstadoCita.CANCELADA.ocupaAgenda());
		assertFalse(EstadoCita.NO_ASISTIO.ocupaAgenda());
	}

	@ParameterizedTest
	@MethodSource("transicionesPermitidas")
	void permiteTransicionesDocumentadasEnSpec(EstadoCita inicial, EstadoCita destino) {
		assertTrue(inicial.puedeTransicionarA(destino));
	}

	@ParameterizedTest
	@MethodSource("transicionesInvalidas")
	void rechazaTransicionesNoPermitidas(EstadoCita inicial, EstadoCita destino) {
		assertFalse(inicial.puedeTransicionarA(destino));
	}

	private static Stream<Arguments> transicionesPermitidas() {
		return Stream.of(
				Arguments.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA),
				Arguments.of(EstadoCita.PENDIENTE, EstadoCita.CANCELADA),
				Arguments.of(EstadoCita.CONFIRMADA, EstadoCita.COMPLETADA),
				Arguments.of(EstadoCita.CONFIRMADA, EstadoCita.CANCELADA),
				Arguments.of(EstadoCita.CONFIRMADA, EstadoCita.NO_ASISTIO));
	}

	private static Stream<Arguments> transicionesInvalidas() {
		return Stream.of(
				Arguments.of(EstadoCita.PENDIENTE, EstadoCita.PENDIENTE),
				Arguments.of(EstadoCita.PENDIENTE, EstadoCita.COMPLETADA),
				Arguments.of(EstadoCita.PENDIENTE, EstadoCita.NO_ASISTIO),
				Arguments.of(EstadoCita.PENDIENTE, null),
				Arguments.of(EstadoCita.CONFIRMADA, EstadoCita.CONFIRMADA),
				Arguments.of(EstadoCita.CONFIRMADA, EstadoCita.PENDIENTE),
				Arguments.of(EstadoCita.COMPLETADA, EstadoCita.CONFIRMADA),
				Arguments.of(EstadoCita.COMPLETADA, EstadoCita.CANCELADA),
				Arguments.of(EstadoCita.COMPLETADA, EstadoCita.NO_ASISTIO),
				Arguments.of(EstadoCita.CANCELADA, EstadoCita.CONFIRMADA),
				Arguments.of(EstadoCita.CANCELADA, EstadoCita.COMPLETADA),
				Arguments.of(EstadoCita.NO_ASISTIO, EstadoCita.CONFIRMADA),
				Arguments.of(EstadoCita.NO_ASISTIO, EstadoCita.COMPLETADA));
	}

}
