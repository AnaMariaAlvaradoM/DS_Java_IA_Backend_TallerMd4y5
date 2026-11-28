package com.devSenior.sillon.ia.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devSenior.sillon.compartido.excepcion.IntegracionInvalidaException;
import com.devSenior.sillon.compartido.excepcion.ServicioNoDisponibleException;
import com.devSenior.sillon.ia.dto.FacturaIa;
import com.devSenior.sillon.ia.dto.ResultadoIa;
import com.devSenior.sillon.ia.proveedor.ProveedorCaidoException;
import com.devSenior.sillon.ia.proveedor.ProveedorClaude;
import com.devSenior.sillon.ia.proveedor.ProveedorGemini;

@ExtendWith(MockitoExtension.class)
class AsistenteIaTest {

	@Mock
	private ProveedorGemini principal;

	@Mock
	private ProveedorClaude respaldo;

	@Test
	void usaElPrincipalCuandoResponde() {
		ResultadoIa esperado = resultado("gemini");
		when(principal.completar("prompt", List.of())).thenReturn(esperado);
		assertEquals("gemini", new AsistenteIa(principal, respaldo).redactar("prompt", List.of()).factura().proveedor());
	}

	@Test
	void usaElRespaldoSiElPrincipalCae() {
		when(principal.nombre()).thenReturn("gemini");
		when(principal.completar("prompt", List.of())).thenThrow(new ProveedorCaidoException());
		when(respaldo.completar("prompt", List.of())).thenReturn(resultado("claude"));
		assertEquals("claude", new AsistenteIa(principal, respaldo).redactar("prompt", List.of()).factura().proveedor());
	}

	@Test
	void responde503SiTodosCaenY502SiElJsonNoSirve() {
		when(principal.nombre()).thenReturn("gemini");
		when(respaldo.nombre()).thenReturn("claude");
		when(principal.completar("caido", List.of())).thenThrow(new ProveedorCaidoException());
		when(respaldo.completar("caido", List.of())).thenThrow(new ProveedorCaidoException());
		assertThrows(ServicioNoDisponibleException.class,
				() -> new AsistenteIa(principal, respaldo).redactar("caido", List.of()));

		when(principal.completar("malo", List.of())).thenThrow(new IntegracionInvalidaException("json"));
		when(respaldo.completar("malo", List.of())).thenThrow(new IntegracionInvalidaException("json"));
		assertThrows(IntegracionInvalidaException.class,
				() -> new AsistenteIa(principal, respaldo).redactar("malo", List.of()));
	}

	private ResultadoIa resultado(String proveedor) {
		return new ResultadoIa("{\"mensaje\":\"hola\"}",
				new FacturaIa(proveedor, "modelo", 1, 1, BigDecimal.ZERO, 5));
	}

}
