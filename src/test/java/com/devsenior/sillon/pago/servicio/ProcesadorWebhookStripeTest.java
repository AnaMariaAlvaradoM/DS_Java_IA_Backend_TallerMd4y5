
package com.devsenior.sillon.pago.servicio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devsenior.sillon.compartido.excepcion.ExcepcionIntegracion;
import com.devsenior.sillon.pago.dto.RespuestaWebhook;
import com.devsenior.sillon.pago.repositorio.EventoStripeRepositorio;
import com.devsenior.sillon.salon.modelo.Plan;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;

class ProcesadorWebhookStripeTest {

    private EventoStripeRepositorio eventoRepositorio;
    private SalonRepositorio salonRepositorio;
    private ProcesadorWebhookStripe procesador;

    @BeforeEach
    void preparar() {

        eventoRepositorio = mock(EventoStripeRepositorio.class);
        salonRepositorio = mock(SalonRepositorio.class);

        Clock reloj = Clock.fixed(
                Instant.parse("2026-11-25T15:00:00Z"),
                ZoneId.of("America/Bogota"));

        procesador = new ProcesadorWebhookStripe(
                eventoRepositorio,
                salonRepositorio,
                reloj);
    }

    @Test
    void eventoDuplicadoNoVuelveAProcesarse() {

        Salon salon = new Salon();
        salon.setId(1L);
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(210);

        Session sesion = mock(Session.class);
        when(sesion.getPaymentStatus()).thenReturn("paid");
        when(sesion.getMetadata()).thenReturn(
                Map.of(
                        "salonId", "1",
                        "tipoCompra", "PAQUETE_CREDITOS"));

        Event evento = eventoConSesion(
                "evt_duplicado", sesion);

        when(salonRepositorio.buscarBloqueadoPorId(1L))
                .thenReturn(Optional.of(salon));

        when(eventoRepositorio.existsById("evt_duplicado"))
                .thenReturn(true);

        RespuestaWebhook respuesta = procesador.procesar(evento);

        assertTrue(respuesta.recibido());
        assertFalse(respuesta.procesado());
        assertEquals(210, salon.getCreditosIa());
        assertEquals(Plan.GRATIS, salon.getPlan());

        verify(salonRepositorio).buscarBloqueadoPorId(1L);
        verify(eventoRepositorio).existsById("evt_duplicado");
        verify(salonRepositorio, never()).save(any());
        verify(eventoRepositorio, never()).saveAndFlush(any());
    }

    @Test
    void eventoDesconocidoNoModificaDatos() {

        Event evento = mock(Event.class);
        when(evento.getId()).thenReturn("evt_desconocido");
        when(evento.getType()).thenReturn("evento.no.soportado");

        RespuestaWebhook respuesta = procesador.procesar(evento);

        assertTrue(respuesta.recibido());
        assertFalse(respuesta.procesado());

        verifyNoInteractions(
                eventoRepositorio,
                salonRepositorio);
    }

    @Test
    void eventoSinIdentificadorEsRechazado() {

        Event evento = mock(Event.class);
        when(evento.getId()).thenReturn(null);

        assertThrows(
                ExcepcionIntegracion.class,
                () -> procesador.procesar(evento));

        verifyNoInteractions(
                eventoRepositorio,
                salonRepositorio);
    }

    @Test
    void pagoConfirmadoActivaProYAgrega300Creditos() {

        Salon salon = new Salon();
        salon.setId(1L);
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(10);

        Session sesion = mock(Session.class);
        when(sesion.getPaymentStatus()).thenReturn("paid");
        when(sesion.getMetadata()).thenReturn(
                Map.of(
                        "salonId", "1",
                        "tipoCompra", "SUSCRIPCION_PRO"));
        when(sesion.getSubscription()).thenReturn("sub_123");

        Event evento = eventoConSesion(
                "evt_pro_123", sesion);

        when(salonRepositorio.buscarBloqueadoPorId(1L))
                .thenReturn(Optional.of(salon));

        RespuestaWebhook respuesta = procesador.procesar(evento);

        assertTrue(respuesta.recibido());
        assertTrue(respuesta.procesado());

        assertEquals(Plan.PRO, salon.getPlan());
        assertEquals(310, salon.getCreditosIa());
        assertEquals("sub_123", salon.getStripeSuscripcionId());

        verify(salonRepositorio).buscarBloqueadoPorId(1L);
        verify(eventoRepositorio).existsById("evt_pro_123");
        verify(salonRepositorio).save(salon);
        verify(eventoRepositorio).saveAndFlush(
                argThat(registro ->
                        "evt_pro_123".equals(registro.getId())
                        && "checkout.session.completed"
                                .equals(registro.getTipo())));
    }

    @Test
    void pagoDePaqueteAgrega200CreditosSinCambiarPlan() {

        Salon salon = new Salon();
        salon.setId(1L);
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(10);

        Session sesion = mock(Session.class);
        when(sesion.getPaymentStatus()).thenReturn("paid");
        when(sesion.getMetadata()).thenReturn(
                Map.of(
                        "salonId", "1",
                        "tipoCompra", "PAQUETE_CREDITOS"));

        Event evento = eventoConSesion(
                "evt_creditos_123", sesion);

        when(salonRepositorio.buscarBloqueadoPorId(1L))
                .thenReturn(Optional.of(salon));

        RespuestaWebhook respuesta = procesador.procesar(evento);

        assertTrue(respuesta.recibido());
        assertTrue(respuesta.procesado());

        assertEquals(Plan.GRATIS, salon.getPlan());
        assertEquals(210, salon.getCreditosIa());
        assertNull(salon.getStripeSuscripcionId());

        verify(salonRepositorio).buscarBloqueadoPorId(1L);
        verify(eventoRepositorio).existsById("evt_creditos_123");
        verify(salonRepositorio).save(salon);
        verify(eventoRepositorio).saveAndFlush(
                argThat(registro ->
                        "evt_creditos_123".equals(registro.getId())
                        && "checkout.session.completed"
                                .equals(registro.getTipo())));
    }

    @Test
    void cancelacionDeSuscripcionConservaCreditos() {

        Salon salon = new Salon();
        salon.setId(1L);
        salon.setPlan(Plan.PRO);
        salon.setCreditosIa(275);
        salon.setStripeSuscripcionId("sub_123");

        Subscription suscripcion = mock(Subscription.class);
        when(suscripcion.getId()).thenReturn("sub_123");

        EventDataObjectDeserializer deserializador =
                mock(EventDataObjectDeserializer.class);

        when(deserializador.getObject())
                .thenReturn(Optional.of(suscripcion));

        Event evento = mock(Event.class);
        when(evento.getId()).thenReturn("evt_cancelacion_123");
        when(evento.getType())
                .thenReturn("customer.subscription.deleted");
        when(evento.getDataObjectDeserializer())
                .thenReturn(deserializador);

        when(salonRepositorio
                .buscarBloqueadoPorSuscripcionId("sub_123"))
                .thenReturn(Optional.of(salon));

        RespuestaWebhook respuesta = procesador.procesar(evento);

        assertTrue(respuesta.recibido());
        assertTrue(respuesta.procesado());

        assertEquals(Plan.GRATIS, salon.getPlan());
        assertEquals(275, salon.getCreditosIa());
        assertNull(salon.getStripeSuscripcionId());

        verify(salonRepositorio)
                .buscarBloqueadoPorSuscripcionId("sub_123");
        verify(eventoRepositorio)
                .existsById("evt_cancelacion_123");
        verify(salonRepositorio).save(salon);
        verify(eventoRepositorio).saveAndFlush(
                argThat(registro ->
                        "evt_cancelacion_123".equals(registro.getId())
                        && "customer.subscription.deleted"
                                .equals(registro.getTipo())));
    }

    private Event eventoConSesion(
            String eventoId,
            Session sesion) {

        EventDataObjectDeserializer deserializador =
                mock(EventDataObjectDeserializer.class);

        when(deserializador.getObject())
                .thenReturn(Optional.of(sesion));

        Event evento = mock(Event.class);
        when(evento.getId()).thenReturn(eventoId);
        when(evento.getType())
                .thenReturn("checkout.session.completed");
        when(evento.getDataObjectDeserializer())
                .thenReturn(deserializador);

        return evento;
    }
}
