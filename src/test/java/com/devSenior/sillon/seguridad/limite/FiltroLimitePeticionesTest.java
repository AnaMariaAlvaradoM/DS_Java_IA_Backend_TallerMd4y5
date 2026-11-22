package com.devSenior.sillon.seguridad.limite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

class FiltroLimitePeticionesTest {

	private final Clock reloj = Clock.fixed(Instant.parse("2026-11-19T15:00:00Z"), ZoneOffset.UTC);

	@Test
	void laPeticion31DelPublicoResponde429() throws Exception {
		FiltroLimitePeticiones filtro = new FiltroLimitePeticiones(reloj, "");
		FilterChain cadena = (peticion, respuesta) -> ((jakarta.servlet.http.HttpServletResponse) respuesta).setStatus(200);
		for (int i = 0; i < 30; i++) {
			MockHttpServletResponse respuesta = new MockHttpServletResponse();
			filtro.doFilter(peticionPublica(), respuesta, cadena);
			assertEquals(200, respuesta.getStatus());
		}
		MockHttpServletResponse bloqueada = new MockHttpServletResponse();
		filtro.doFilter(peticionPublica(), bloqueada, cadena);
		assertEquals(429, bloqueada.getStatus());
		assertEquals(true, bloqueada.getContentAsString().contains("\"codigo\":429"));
	}

	@Test
	void elRegistroNoCuentaEnElLimite() throws Exception {
		FiltroLimitePeticiones filtro = new FiltroLimitePeticiones(reloj, "");
		FilterChain cadena = org.mockito.Mockito.mock(FilterChain.class);
		for (int i = 0; i < 40; i++) {
			MockHttpServletRequest peticion = new MockHttpServletRequest("POST", "/api/auth/registro");
			peticion.setRemoteAddr("10.1.1.1");
			filtro.doFilter(peticion, new MockHttpServletResponse(), cadena);
		}
		verify(cadena, org.mockito.Mockito.times(40)).doFilter(org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any());
	}

	@Test
	void elLoginSiCuentaEnElLimite() throws Exception {
		FiltroLimitePeticiones filtro = new FiltroLimitePeticiones(reloj, "");
		FilterChain cadena = (peticion, respuesta) -> ((jakarta.servlet.http.HttpServletResponse) respuesta).setStatus(200);
		MockHttpServletResponse ultima = new MockHttpServletResponse();
		for (int i = 0; i < 31; i++) {
			ultima = new MockHttpServletResponse();
			MockHttpServletRequest peticion = new MockHttpServletRequest("POST", "/api/auth/login");
			peticion.setRemoteAddr("10.2.2.2");
			filtro.doFilter(peticion, ultima, cadena);
		}
		assertEquals(429, ultima.getStatus());
	}

	private MockHttpServletRequest peticionPublica() {
		MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/api/publico/salones/demo");
		peticion.setRemoteAddr("10.0.0.8");
		return peticion;
	}

}
