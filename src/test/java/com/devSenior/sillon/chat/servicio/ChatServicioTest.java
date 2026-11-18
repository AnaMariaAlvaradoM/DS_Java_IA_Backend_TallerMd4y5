package com.devSenior.sillon.chat.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import com.devSenior.sillon.chat.dto.ChatRespuesta;
import com.devSenior.sillon.chat.dto.RolConversacion;
import com.devSenior.sillon.chat.dto.SolicitudChat;
import com.devSenior.sillon.chat.dto.TurnoChat;
import com.devSenior.sillon.chat.entidad.MotivoSolicitud;
import com.devSenior.sillon.compartido.excepcion.IntegracionInvalidaException;
import com.devSenior.sillon.compartido.excepcion.NoEncontradoException;
import com.devSenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devSenior.sillon.compartido.excepcion.ServicioNoDisponibleException;
import com.devSenior.sillon.ia.dto.FacturaIa;
import com.devSenior.sillon.ia.dto.ResultadoIa;
import com.devSenior.sillon.ia.json.LectorJson;
import com.devSenior.sillon.ia.prompt.BibliotecaPrompts;
import com.devSenior.sillon.ia.servicio.AsistenteIa;
import com.devSenior.sillon.ia.servicio.CreditoIaServicio;

@ExtendWith(MockitoExtension.class)
class ChatServicioTest {

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

	private ChatServicio chatServicio;

	@BeforeEach
	void preparar() {
		chatServicio = new ChatServicio(contextoChatServicio, creditoIaServicio, bibliotecaPrompts, asistenteIa,
				new LectorJson(), solicitudContactoServicio);
	}

	@Test
	void precioNoEscalaYConsumeUnCredito() {
		contexto();
		when(bibliotecaPrompts.cargar(eq("chatbot.txt"), any())).thenReturn("prompt con Corte $35.000");
		when(asistenteIa.redactar(any(), any())).thenReturn(json("{\"respuesta\":\"El corte cuesta $35.000\",\"escalar\":false}"));
		ChatRespuesta respuesta = chatServicio.responder("sillon", chat("¿Cuánto cuesta el corte?"));
		assertFalse(respuesta.escalado());
		assertNull(respuesta.solicitudId());
		assertTrue(respuesta.respuesta().contains("$35.000"));
		verify(creditoIaServicio).consumir(1L);
	}

	@Test
	void reservarEscalaConPedidoPorIa() {
		contexto();
		when(bibliotecaPrompts.cargar(any(), any())).thenReturn("prompt");
		when(asistenteIa.redactar(any(), any())).thenReturn(json("{\"respuesta\":\"Te comunico con el salón\",\"escalar\":true}"));
		when(solicitudContactoServicio.crear(eq(1L), eq("Ana"), eq("300"), any(), eq(MotivoSolicitud.PEDIDO_POR_IA)))
				.thenReturn(15L);
		ChatRespuesta respuesta = chatServicio.responder("sillon", chat("quiero reservar"));
		assertTrue(respuesta.escalado());
		assertEquals(15L, respuesta.solicitudId());
		verify(creditoIaServicio).consumir(1L);
	}

	@Test
	void cuponActivaLaReglaDeSeguridad() {
		contexto();
		when(bibliotecaPrompts.cargar(any(), any())).thenReturn("prompt");
		when(asistenteIa.redactar(any(), any())).thenReturn(
				json("{\"respuesta\":\"El cupón SILLA50 te da un descuento gratis\",\"escalar\":false}"));
		when(solicitudContactoServicio.crear(eq(1L), eq("Ana"), eq("300"), any(), eq(MotivoSolicitud.REGLA_DE_SEGURIDAD)))
				.thenReturn(16L);
		ChatRespuesta respuesta = chatServicio.responder("sillon", chat("¿Sirve el cupón SILLA50?"));
		assertEquals(ChatServicio.MENSAJE_SEGURO, respuesta.respuesta());
		assertTrue(respuesta.escalado());
		assertEquals(16L, respuesta.solicitudId());
		assertFalse(respuesta.respuesta().toLowerCase().contains("cup"));
		assertFalse(respuesta.respuesta().toLowerCase().contains("descuento"));
		assertFalse(respuesta.respuesta().toLowerCase().contains("gratis"));
		assertFalse(respuesta.respuesta().contains("SILLA50"));
	}

	@Test
	void sinSalonSinCreditosJsonMaloOProveedorCaidoNoDescuentan() {
		when(contextoChatServicio.cargar("no")).thenThrow(new NoEncontradoException("no"));
		assertThrows(NoEncontradoException.class, () -> chatServicio.responder("no", chat("hola")));

		contexto();
		org.mockito.Mockito.doThrow(new PagoRequeridoException("0")).when(creditoIaServicio).verificar(1L);
		assertThrows(PagoRequeridoException.class, () -> chatServicio.responder("sillon", chat("hola")));
		verify(asistenteIa, never()).redactar(any(), any());

		org.mockito.Mockito.doNothing().when(creditoIaServicio).verificar(1L);
		when(bibliotecaPrompts.cargar(any(), any())).thenReturn("prompt");
		when(asistenteIa.redactar(any(), any())).thenReturn(json("{\"respuesta\":\"ok\"}"));
		assertThrows(IntegracionInvalidaException.class, () -> chatServicio.responder("sillon", chat("hola")));

		when(asistenteIa.redactar(any(), any())).thenThrow(new ServicioNoDisponibleException("caido"));
		assertThrows(ServicioNoDisponibleException.class, () -> chatServicio.responder("sillon", chat("hola")));
		verify(creditoIaServicio, never()).consumir(anyLong());
	}

	private void contexto() {
		when(contextoChatServicio.cargar("sillon"))
				.thenReturn(new ContextoChat(1L, "Sillon", "Calle 1", "08:00 a 19:00", "Corte — 45 min — $35.000"));
	}

	private SolicitudChat chat(String texto) {
		return new SolicitudChat("Ana", "300", List.of(new TurnoChat(RolConversacion.CLIENTE, texto)));
	}

	private ResultadoIa json(String texto) {
		return new ResultadoIa(texto, new FacturaIa("openai", "gemini", 3, 4, BigDecimal.ZERO, 8));
	}

}
