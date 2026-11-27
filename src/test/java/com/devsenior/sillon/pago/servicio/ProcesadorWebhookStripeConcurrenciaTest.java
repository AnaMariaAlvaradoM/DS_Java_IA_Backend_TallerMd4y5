
package com.devsenior.sillon.pago.servicio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.devsenior.sillon.pago.dto.RespuestaWebhook;
import com.devsenior.sillon.pago.repositorio.EventoStripeRepositorio;
import com.devsenior.sillon.salon.modelo.ConfiguracionSalon;
import com.devsenior.sillon.salon.modelo.Plan;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;

@SpringBootTest
class ProcesadorWebhookStripeConcurrenciaTest {

    @Autowired
    private ProcesadorWebhookStripe procesador;

    @Autowired
    private SalonRepositorio salonRepositorio;

    @Autowired
    private EventoStripeRepositorio eventoRepositorio;

    private Long salonId;
    private String eventoId;

    @AfterEach
    void limpiarDatosDePrueba() {

        if (eventoId != null
                && eventoRepositorio.existsById(eventoId)) {
            eventoRepositorio.deleteById(eventoId);
        }

        if (salonId != null
                && salonRepositorio.existsById(salonId)) {
            salonRepositorio.deleteById(salonId);
        }
    }

    @Test
    void dosWebhooksSimultaneosSoloAplicanUnPago()
            throws Exception {

        Salon salon = new Salon();
        salon.setNombre("Salon Test Concurrencia Stripe");
        salon.setSlug(
                "test-concurrencia-" + UUID.randomUUID());
        salon.setCiudad("Bogota");
        salon.setDireccion("Calle de prueba 123");
        salon.setTelefono("3001234567");
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(10);
        salon.setConfiguracion(
                ConfiguracionSalon.predeterminada());
        salon.setFechaCreacion(
                LocalDateTime.of(2026, 11, 25, 15, 0));

        salonId = salonRepositorio
                .saveAndFlush(salon)
                .getId();

        eventoId = "evt_concurrencia_" + UUID.randomUUID();

        Session sesion = mock(Session.class);
        when(sesion.getPaymentStatus())
                .thenReturn("paid");
        when(sesion.getMetadata())
                .thenReturn(Map.of(
                        "salonId", salonId.toString(),
                        "tipoCompra", "PAQUETE_CREDITOS"));

        EventDataObjectDeserializer deserializador = mock(EventDataObjectDeserializer.class);

        when(deserializador.getObject())
                .thenReturn(Optional.of(sesion));

        Event evento = mock(Event.class);
        when(evento.getId()).thenReturn(eventoId);
        when(evento.getType())
                .thenReturn("checkout.session.completed");
        when(evento.getDataObjectDeserializer())
                .thenReturn(deserializador);

        CountDownLatch preparados = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);

        Callable<RespuestaWebhook> tarea = () -> {
            preparados.countDown();

            if (!iniciar.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "No se inicio la prueba concurrente");
            }

            return procesador.procesar(evento);
        };

        ExecutorService ejecutor = Executors.newFixedThreadPool(2);

        try {
            Future<RespuestaWebhook> primera = ejecutor.submit(tarea);

            Future<RespuestaWebhook> segunda = ejecutor.submit(tarea);

            assertTrue(
                    preparados.await(10, TimeUnit.SECONDS),
                    "Los dos hilos deben estar preparados");

            iniciar.countDown();

            RespuestaWebhook respuesta1 = primera.get(30, TimeUnit.SECONDS);

            RespuestaWebhook respuesta2 = segunda.get(30, TimeUnit.SECONDS);

            assertTrue(respuesta1.recibido());
            assertTrue(respuesta2.recibido());

            int procesados = 0;

            if (respuesta1.procesado()) {
                procesados++;
            }

            if (respuesta2.procesado()) {
                procesados++;
            }

            assertEquals(
                    1,
                    procesados,
                    "Solo un webhook debe aplicar el pago");

            Salon actualizado = salonRepositorio
                    .findById(salonId)
                    .orElseThrow();

            assertEquals(
                    210,
                    actualizado.getCreditosIa(),
                    "Los créditos no deben duplicarse");

            assertEquals(
                    Plan.GRATIS,
                    actualizado.getPlan());

            assertTrue(
                    eventoRepositorio.existsById(eventoId));

        } finally {
            iniciar.countDown();
            ejecutor.shutdownNow();
        }
    }
}
