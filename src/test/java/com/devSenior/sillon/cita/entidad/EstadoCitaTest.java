package com.devSenior.sillon.cita.entidad;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EstadoCitaTest {

	@Test
	void permiteLasCincoTransicionesYOcupaAgenda() {
		assertTrue(EstadoCita.PENDIENTE.puedeCambiarA(EstadoCita.CONFIRMADA));
		assertTrue(EstadoCita.PENDIENTE.puedeCambiarA(EstadoCita.CANCELADA));
		assertTrue(EstadoCita.CONFIRMADA.puedeCambiarA(EstadoCita.COMPLETADA));
		assertTrue(EstadoCita.CONFIRMADA.puedeCambiarA(EstadoCita.CANCELADA));
		assertTrue(EstadoCita.CONFIRMADA.puedeCambiarA(EstadoCita.NO_ASISTIO));
		assertTrue(EstadoCita.PENDIENTE.ocupaAgenda());
		assertTrue(EstadoCita.CONFIRMADA.ocupaAgenda());
		assertFalse(EstadoCita.COMPLETADA.ocupaAgenda());
		assertFalse(EstadoCita.CANCELADA.ocupaAgenda());
		assertFalse(EstadoCita.NO_ASISTIO.ocupaAgenda());
	}

	@Test
	void bloqueaTransicionesFinalesEInvalidas() {
		assertFalse(EstadoCita.PENDIENTE.puedeCambiarA(EstadoCita.COMPLETADA));
		assertFalse(EstadoCita.PENDIENTE.puedeCambiarA(EstadoCita.NO_ASISTIO));
		assertFalse(EstadoCita.PENDIENTE.puedeCambiarA(EstadoCita.PENDIENTE));
		assertFalse(EstadoCita.CONFIRMADA.puedeCambiarA(EstadoCita.PENDIENTE));
		assertFalse(EstadoCita.COMPLETADA.puedeCambiarA(EstadoCita.CANCELADA));
		assertFalse(EstadoCita.CANCELADA.puedeCambiarA(EstadoCita.PENDIENTE));
		assertFalse(EstadoCita.NO_ASISTIO.puedeCambiarA(EstadoCita.CONFIRMADA));
		assertFalse(EstadoCita.CONFIRMADA.puedeCambiarA(null));
	}

}
