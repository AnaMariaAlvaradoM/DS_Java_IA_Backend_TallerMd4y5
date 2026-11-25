
package com.devsenior.sillon.pago.servicio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.pago.dto.RespuestaWebhook;
import com.devsenior.sillon.pago.repositorio.EventoStripeRepositorio;
import com.devsenior.sillon.salon.modelo.ConfiguracionSalon;
import com.devsenior.sillon.salon.modelo.Plan;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class ProcesadorWebhookStripeIntegracionTest {

    private final ProcesadorWebhookStripe procesador;
    private final SalonRepositorio salonRepositorio;
    private final EventoStripeRepositorio eventoRepositorio;
    private final EntityManager entityManager;

    @Autowired
    ProcesadorWebhookStripeIntegracionTest(
            ProcesadorWebhookStripe procesador,
            SalonRepositorio salonRepositorio,
            EventoStripeRepositorio eventoRepositorio,
            EntityManager entityManager) {

        this.procesador = procesador;
        this.salonRepositorio = salonRepositorio;
        this.eventoRepositorio = eventoRepositorio;
        this.entityManager = entityManager;
    }

    @Test
    void pagoDeCreditosSePersisteUnaSolaVezEnMySql() {

        Salon salon = new Salon();
        salon.setNombre("Salon Prueba Stripe");
        salon.setSlug("test-stripe-" + UUID.randomUUID());
        salon.setCiudad("Bogota");
        salon.setDireccion("Calle de prueba 123");
        salon.setTelefono("3001234567");
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(10);
        salon.setConfiguracion(
                ConfiguracionSalon.predeterminada());
        salon.setFechaCreacion(
                LocalDateTime.of(2026, 11, 25, 14, 0));

        salon = salonRepositorio.saveAndFlush(salon);
        Long salonId = salon.getId();

        String eventoId = "evt_test_" + UUID.randomUUID();

        Session sesion = mock(Session.class);
        when(sesion.getPaymentStatus()).thenReturn("paid");
        when(sesion.getMetadata()).thenReturn(
                Map.of(
                        "salonId", salonId.toString(),
                        "tipoCompra", "PAQUETE_CREDITOS"));

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

        RespuestaWebhook primera = procesador.procesar(evento);

        assertTrue(primera.recibido());
        assertTrue(primera.procesado());

        entityManager.flush();
        entityManager.clear();

        Salon despuesDelPago = salonRepositorio.findById(salonId)
                .orElseThrow();

        assertEquals(Plan.GRATIS, despuesDelPago.getPlan());
        assertEquals(210, despuesDelPago.getCreditosIa());

        assertTrue(eventoRepositorio.existsById(eventoId));

        RespuestaWebhook segunda = procesador.procesar(evento);

        assertTrue(segunda.recibido());
        assertFalse(segunda.procesado());

        entityManager.flush();
        entityManager.clear();

        Salon despuesDelDuplicado =
                salonRepositorio.findById(salonId)
                        .orElseThrow();

        assertEquals(
                210,
                despuesDelDuplicado.getCreditosIa());

        assertEquals(
                Plan.GRATIS,
                despuesDelDuplicado.getPlan());

        assertTrue(eventoRepositorio.existsById(eventoId));
    }
}
