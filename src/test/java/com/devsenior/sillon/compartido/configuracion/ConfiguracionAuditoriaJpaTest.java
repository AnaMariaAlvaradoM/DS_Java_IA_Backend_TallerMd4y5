package com.devsenior.sillon.compartido.configuracion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.data.auditing.DateTimeProvider;

class ConfiguracionAuditoriaJpaTest {

	@Test
	void proveedorFechaAuditoriaConservaPrecisionParaActualizacionInmediataEnElMismoSegundo() {
		ZoneId zonaBogota = ZoneId.of("America/Bogota");
		AtomicReference<Instant> instanteActual = new AtomicReference<>(
				Instant.parse("2026-11-24T15:30:00.100Z"));

		Clock relojControlado = new Clock() {
			@Override
			public ZoneId getZone() {
				return zonaBogota;
			}

			@Override
			public Clock withZone(ZoneId zone) {
				return this;
			}

			@Override
			public Instant instant() {
				return instanteActual.get();
			}
		};

		DateTimeProvider proveedor = new ConfiguracionAuditoriaJpa()
				.proveedorFechaAuditoria(relojControlado);

		LocalDateTime creacion = (LocalDateTime) proveedor.getNow().orElseThrow();
		instanteActual.set(Instant.parse("2026-11-24T15:30:00.250Z"));
		LocalDateTime actualizacionInmediata = (LocalDateTime) proveedor.getNow().orElseThrow();

		assertEquals(LocalDateTime.of(2026, 11, 24, 10, 30, 0, 100_000_000), creacion);
		assertTrue(actualizacionInmediata.isAfter(creacion));
	}

}
