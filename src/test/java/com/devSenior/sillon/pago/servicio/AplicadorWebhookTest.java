package com.devSenior.sillon.pago.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devSenior.sillon.pago.dto.EventoPago;
import com.devSenior.sillon.pago.dto.WebhookRespuesta;
import com.devSenior.sillon.pago.entidad.TipoCompra;
import com.devSenior.sillon.pago.repositorio.EventoStripeRepositorio;
import com.devSenior.sillon.salon.entidad.Plan;
import com.devSenior.sillon.salon.entidad.Salon;
import com.devSenior.sillon.salon.repositorio.SalonRepositorio;

@ExtendWith(MockitoExtension.class)
class AplicadorWebhookTest {

	@Mock
	private EventoStripeRepositorio eventoStripeRepositorio;

	@Mock
	private SalonRepositorio salonRepositorio;

	private AplicadorWebhook aplicador;

	@BeforeEach
	void preparar() {
		Clock reloj = Clock.fixed(Instant.parse("2026-11-18T15:00:00Z"), ZoneOffset.UTC);
		aplicador = new AplicadorWebhook(eventoStripeRepositorio, salonRepositorio, reloj, 300, 200);
	}

	@Test
	void suscripcionActivaProYSumaTrescientos() {
		Salon salon = salon(10, Plan.GRATIS);
		when(eventoStripeRepositorio.existsById("evt_pro")).thenReturn(false);
		when(salonRepositorio.findById(4L)).thenReturn(Optional.of(salon));

		WebhookRespuesta respuesta = aplicador.aplicar(new EventoPago(
				"evt_pro", "checkout.session.completed", "paid", TipoCompra.SUSCRIPCION_PRO, 4L, "sub_1"));

		assertTrue(respuesta.recibido());
		assertTrue(respuesta.procesado());
		assertEquals(Plan.PRO, salon.getPlan());
		assertEquals(310, salon.getCreditosIa());
		assertEquals("sub_1", salon.getStripeSuscripcionId());
	}

	@Test
	void paqueteSumaDoscientosSinCambiarElPlan() {
		Salon salon = salon(10, Plan.GRATIS);
		when(eventoStripeRepositorio.existsById("evt_pack")).thenReturn(false);
		when(salonRepositorio.findById(4L)).thenReturn(Optional.of(salon));

		WebhookRespuesta respuesta = aplicador.aplicar(new EventoPago(
				"evt_pack", "checkout.session.completed", "paid", TipoCompra.PAQUETE_CREDITOS, 4L, null));

		assertTrue(respuesta.procesado());
		assertEquals(Plan.GRATIS, salon.getPlan());
		assertEquals(210, salon.getCreditosIa());
	}

	@Test
	void eventoRepetidoNoSumaCreditos() {
		when(eventoStripeRepositorio.existsById("evt_pro")).thenReturn(true);

		WebhookRespuesta respuesta = aplicador.aplicar(new EventoPago(
				"evt_pro", "checkout.session.completed", "paid", TipoCompra.SUSCRIPCION_PRO, 4L, "sub_1"));

		assertTrue(respuesta.recibido());
		assertFalse(respuesta.procesado());
		verify(salonRepositorio, never()).findById(4L);
		verify(eventoStripeRepositorio, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void cancelacionVuelveAGratisYConservaCreditos() {
		Salon salon = salon(80, Plan.PRO);
		salon.setStripeSuscripcionId("sub_1");
		when(eventoStripeRepositorio.existsById("evt_del")).thenReturn(false);
		when(salonRepositorio.findByStripeSuscripcionId("sub_1")).thenReturn(Optional.of(salon));

		WebhookRespuesta respuesta = aplicador.aplicar(
				new EventoPago("evt_del", "customer.subscription.deleted", null, null, null, "sub_1"));

		assertTrue(respuesta.procesado());
		assertEquals(Plan.GRATIS, salon.getPlan());
		assertEquals(80, salon.getCreditosIa());
		assertNull(salon.getStripeSuscripcionId());
	}

	@Test
	void pagoNoConfirmadoNoCambiaElSalon() {
		when(eventoStripeRepositorio.existsById("evt_unpaid")).thenReturn(false);

		WebhookRespuesta respuesta = aplicador.aplicar(new EventoPago(
				"evt_unpaid", "checkout.session.completed", "unpaid", TipoCompra.SUSCRIPCION_PRO, 4L, "sub_1"));

		assertFalse(respuesta.procesado());
		verify(salonRepositorio, never()).findById(4L);
	}

	@Test
	void eventoDesconocidoSeRegistraSinCambiarElSalon() {
		when(eventoStripeRepositorio.existsById("evt_otro")).thenReturn(false);

		WebhookRespuesta respuesta = aplicador.aplicar(
				new EventoPago("evt_otro", "charge.succeeded", null, null, null, null));

		assertFalse(respuesta.procesado());
		verify(eventoStripeRepositorio).saveAndFlush(org.mockito.ArgumentMatchers.any());
		verify(salonRepositorio, never()).findById(org.mockito.ArgumentMatchers.any());
	}

	private Salon salon(int creditos, Plan plan) {
		Salon salon = new Salon();
		salon.setId(4L);
		salon.setCreditosIa(creditos);
		salon.setPlan(plan);
		return salon;
	}

}
