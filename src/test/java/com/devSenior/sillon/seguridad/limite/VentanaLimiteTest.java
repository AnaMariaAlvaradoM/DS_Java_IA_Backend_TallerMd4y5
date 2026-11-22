package com.devSenior.sillon.seguridad.limite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class VentanaLimiteTest {

	@Test
	void permiteTreintaYRechazaLaSiguiente() {
		VentanaLimite limite = new VentanaLimite(30, Duration.ofMinutes(1));
		Instant ahora = Instant.parse("2026-11-19T15:00:00Z");
		for (int i = 0; i < 30; i++) {
			assertTrue(limite.permitir("10.0.0.8", ahora));
		}
		assertFalse(limite.permitir("10.0.0.8", ahora));
	}

	@Test
	void laVentanaSeReinicia() {
		VentanaLimite limite = new VentanaLimite(30, Duration.ofMinutes(1));
		Instant ahora = Instant.parse("2026-11-19T15:00:00Z");
		for (int i = 0; i < 30; i++) {
			limite.permitir("10.0.0.8", ahora);
		}
		assertFalse(limite.permitir("10.0.0.8", ahora.plusSeconds(59)));
		assertTrue(limite.permitir("10.0.0.8", ahora.plusSeconds(60)));
	}

	@Test
	void cadaIpTieneSuPropioContador() {
		VentanaLimite limite = new VentanaLimite(30, Duration.ofMinutes(1));
		Instant ahora = Instant.parse("2026-11-19T15:00:00Z");
		for (int i = 0; i < 30; i++) {
			limite.permitir("10.0.0.8", ahora);
		}
		assertTrue(limite.permitir("10.0.0.9", ahora));
		assertFalse(limite.permitir("10.0.0.8", ahora));
	}

	@Test
	void ignoraLaCabeceraSiElRemitenteNoEsProxy() {
		ResolvedorIp resolvedor = new ResolvedorIp("");
		org.springframework.mock.web.MockHttpServletRequest peticion = new org.springframework.mock.web.MockHttpServletRequest();
		peticion.setRemoteAddr("10.0.0.8");
		peticion.addHeader("X-Forwarded-For", "1.2.3.4");
		assertEquals("10.0.0.8", resolvedor.ip(peticion));
	}

	@Test
	void usaLaUltimaIpCuandoElRemitenteEsElProxy() {
		ResolvedorIp resolvedor = new ResolvedorIp("10.0.0.1");
		org.springframework.mock.web.MockHttpServletRequest peticion = new org.springframework.mock.web.MockHttpServletRequest();
		peticion.setRemoteAddr("10.0.0.1");
		peticion.addHeader("X-Forwarded-For", "9.9.9.9, 203.0.113.8");
		assertEquals("203.0.113.8", resolvedor.ip(peticion));
	}

}
