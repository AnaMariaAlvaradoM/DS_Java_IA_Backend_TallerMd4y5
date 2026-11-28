package com.devSenior.sillon.chat.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devSenior.sillon.chat.dto.RolConversacion;
import com.devSenior.sillon.chat.dto.SolicitudChat;
import com.devSenior.sillon.chat.dto.TurnoChat;
import com.devSenior.sillon.chat.entidad.MotivoSolicitud;
import com.devSenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devSenior.sillon.compartido.excepcion.IntegracionInvalidaException;
import com.devSenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devSenior.sillon.compartido.excepcion.ServicioNoDisponibleException;
import com.devSenior.sillon.ia.dto.FacturaIa;
import com.devSenior.sillon.ia.dto.MensajeIa;
import com.devSenior.sillon.ia.dto.ResultadoIa;
import com.devSenior.sillon.ia.json.LectorJson;
import com.devSenior.sillon.ia.proveedor.ProveedorCaidoException;
import com.devSenior.sillon.ia.proveedor.ProveedorClaude;
import com.devSenior.sillon.ia.proveedor.ProveedorGemini;
import com.devSenior.sillon.ia.prompt.BibliotecaPrompts;
import com.devSenior.sillon.ia.servicio.AsistenteIa;
import com.devSenior.sillon.ia.servicio.CreditoIaServicio;

@ExtendWith(MockitoExtension.class)
class ChatbotCasosTest {

	@Mock
	private ContextoChatServicio contextoChatServicio;
	@Mock
	private CreditoIaServicio creditoIaServicio;
	@Mock
	private BibliotecaPrompts bibliotecaPrompts;
	@Mock
	private AsistenteIa asistenteIa;
	@Mock
	private SolicitudContactoServicio solicitudContactoServicio;
	@Mock
	private ProveedorGemini gemini;
	@Mock
	private ProveedorClaude claude;

	private ChatServicio chatServicio;

	@BeforeEach
	void preparar() {
		chatServicio = new ChatServicio(contextoChatServicio, creditoIaServicio, bibliotecaPrompts, asistenteIa,
				new LectorJson(), solicitudContactoServicio);
	}

	@Test
	void consultaValidaDevuelveLaRespuestaYConsumeUnCredito() {
		contexto();
		when(bibliotecaPrompts.cargar(any(), any())).thenReturn("prompt");
		when(asistenteIa.redactar(any(), any())).thenReturn(resultado("{\"respuesta\":\"El corte cuesta 35000.\",\"escalar\":false}"));
		var respuesta = chatServicio.responder("sillon", chat("precio"));
		assertEquals("El corte cuesta 35000.", respuesta.respuesta());
		assertFalse(respuesta.escalado());
		verify(creditoIaServicio).consumir(1L);
	}

	@Test
	void historialVacioNoLlamaALaIa() {
		assertThrows(FirmaInvalidaException.class,
				() -> chatServicio.responder("sillon", new SolicitudChat("Ana", "300", List.of())));
		verify(asistenteIa, never()).redactar(any(), any());
		verify(creditoIaServicio, never()).consumir(anyLong());
	}

	@Test
	void jsonInservibleNoDescuenta() {
		contexto();
		when(bibliotecaPrompts.cargar(any(), any())).thenReturn("prompt");
		when(asistenteIa.redactar(any(), any())).thenReturn(resultado("no-json"));
		assertThrows(IntegracionInvalidaException.class, () -> chatServicio.responder("sillon", chat("hola")));
		verify(creditoIaServicio, never()).consumir(anyLong());
	}

	@Test
	void elRespaldoCubreLaCaidaDelPrincipal() {
		when(gemini.nombre()).thenReturn("gemini");
		when(gemini.completar(any(), any())).thenThrow(new ProveedorCaidoException());
		when(claude.completar(any(), any())).thenReturn(resultado("{\"respuesta\":\"ok\",\"escalar\":false}"));
		ResultadoIa resultado = new AsistenteIa(gemini, claude).redactar("prompt", List.of(new MensajeIa("user", "hola")));
		assertTrue(resultado.texto().contains("ok"));
	}

	@Test
	void timeoutYErrorHttpDelProveedorTerminanEn503SinDescontar() {
		when(gemini.nombre()).thenReturn("gemini");
		when(claude.nombre()).thenReturn("claude");
		when(gemini.completar(any(), any())).thenThrow(new ProveedorCaidoException());
		when(claude.completar(any(), any())).thenThrow(new ProveedorCaidoException());
		assertThrows(ServicioNoDisponibleException.class,
				() -> new AsistenteIa(gemini, claude).redactar("prompt", List.of(new MensajeIa("user", "hola"))));

		contexto();
		when(bibliotecaPrompts.cargar(any(), any())).thenReturn("prompt");
		when(asistenteIa.redactar(any(), any())).thenThrow(new ServicioNoDisponibleException("caido"));
		assertThrows(ServicioNoDisponibleException.class, () -> chatServicio.responder("sillon", chat("hola")));
		verify(creditoIaServicio, never()).consumir(anyLong());
	}

	@Test
	void escalarCreaSolicitudDePersona() {
		contexto();
		when(bibliotecaPrompts.cargar(any(), any())).thenReturn("prompt");
		when(asistenteIa.redactar(any(), any())).thenReturn(resultado("{\"respuesta\":\"Le comunico con el salon.\",\"escalar\":true}"));
		when(solicitudContactoServicio.crear(eq(1L), eq("Ana"), eq("300"), any(), eq(MotivoSolicitud.PEDIDO_POR_IA)))
				.thenReturn(9L);
		assertEquals(9L, chatServicio.responder("sillon", chat("quiero una persona")).solicitudId());
	}

	@Test
	void unaExcepcionInesperadaNoDescuenta() {
		contexto();
		when(bibliotecaPrompts.cargar(any(), any())).thenReturn("prompt");
		when(asistenteIa.redactar(any(), any())).thenThrow(new IllegalStateException("inesperada"));
		assertThrows(IllegalStateException.class, () -> chatServicio.responder("sillon", chat("hola")));
		verify(creditoIaServicio, never()).consumir(anyLong());
	}

	@Test
	void sinCreditosNoLlamaALaIa() {
		contexto();
		org.mockito.Mockito.doThrow(new PagoRequeridoException("0")).when(creditoIaServicio).verificar(1L);
		assertThrows(PagoRequeridoException.class, () -> chatServicio.responder("sillon", chat("hola")));
		verify(asistenteIa, never()).redactar(any(), any());
	}

	private void contexto() {
		when(contextoChatServicio.cargar("sillon"))
				.thenReturn(new ContextoChat(1L, "Sillon", "Calle 1", "08:00 a 19:00", "Corte"));
	}

	private SolicitudChat chat(String texto) {
		return new SolicitudChat("Ana", "300", List.of(new TurnoChat(RolConversacion.CLIENTE, texto)));
	}

	private ResultadoIa resultado(String texto) {
		return new ResultadoIa(texto, new FacturaIa("gemini", "gemini", 1, 1, BigDecimal.ZERO, 1));
	}

}
