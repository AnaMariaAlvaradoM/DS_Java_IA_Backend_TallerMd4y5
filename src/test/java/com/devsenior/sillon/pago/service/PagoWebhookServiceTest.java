package com.devsenior.sillon.pago.service;

import com.devsenior.sillon.compartido.FirmaStripeInvalidaException;
import com.devsenior.sillon.pago.config.StripeProperties;
import com.devsenior.sillon.pago.model.EventoStripe;
import com.devsenior.sillon.pago.repository.EventoStripeRepository;
import com.devsenior.sillon.salon.model.Plan;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.repository.SalonRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Subscription;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoWebhookServiceTest {
    private static final String PAYLOAD = "{}";
    private static final String FIRMA = "firma-de-prueba";
    private static final String SECRETO = "whsec-prueba";

    @Mock private EventoStripeRepository eventoStripeRepository;
    @Mock private SalonRepository salonRepository;

    private PagoWebhookService webhookService;
    private StripeProperties stripeProperties;

    @BeforeEach
    void prepararServicio() {
        stripeProperties = new StripeProperties();
        stripeProperties.setWebhookSecret(SECRETO);
        webhookService = new PagoWebhookService(stripeProperties, eventoStripeRepository, salonRepository);
    }

    @Test
    void pagoProActivaPlanYAgregaTrescientosCreditos() throws Exception {
        Salon salon = salonConPlanYCreditos(Plan.GRATIS, 10);
        Session session = mockSession("SUSCRIPCION_PRO", "sub-prueba");
        Event evento = eventoPago("evt-pro", session);
        when(eventoStripeRepository.existsById("evt-pro")).thenReturn(false);
        when(salonRepository.findById(55L)).thenReturn(Optional.of(salon));

        try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
            webhook.when(() -> Webhook.constructEvent(PAYLOAD, FIRMA, SECRETO)).thenReturn(evento);

            webhookService.procesarEvento(PAYLOAD, FIRMA);
        }

        assertEquals(Plan.PRO, salon.getPlan());
        assertEquals(310, salon.getCreditosIa());
        assertEquals("sub-prueba", salon.getSuscripcionStripeId());
        verify(salonRepository).save(salon);
        verify(eventoStripeRepository).save(any(EventoStripe.class));
    }

    @Test
    void compraDePaqueteAgregaCreditosSinCambiarElPlan() throws Exception {
        Salon salon = salonConPlanYCreditos(Plan.GRATIS, 15);
        Session session = mockSession("PAQUETE_CREDITOS", null);
        Event evento = eventoPago("evt-paquete", session);
        when(eventoStripeRepository.existsById("evt-paquete")).thenReturn(false);
        when(salonRepository.findById(55L)).thenReturn(Optional.of(salon));

        try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
            webhook.when(() -> Webhook.constructEvent(PAYLOAD, FIRMA, SECRETO)).thenReturn(evento);

            webhookService.procesarEvento(PAYLOAD, FIRMA);
        }

        assertEquals(Plan.GRATIS, salon.getPlan());
        assertEquals(215, salon.getCreditosIa());
        verify(salonRepository).save(salon);
    }

    @Test
    void eventoRepetidoNoVuelveAAplicarCompraNiGuardarOtraVez() throws Exception {
        Event evento = mock(Event.class);
        when(evento.getId()).thenReturn("evt-repetido");
        when(eventoStripeRepository.existsById("evt-repetido")).thenReturn(true);

        try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
            webhook.when(() -> Webhook.constructEvent(PAYLOAD, FIRMA, SECRETO)).thenReturn(evento);

            Map<String, Boolean> resultado = webhookService.procesarEvento(PAYLOAD, FIRMA);

            assertEquals(Map.of("recibido", true, "procesado", false), resultado);
        }

        verify(salonRepository, never()).save(any(Salon.class));
        verify(eventoStripeRepository, never()).save(any(EventoStripe.class));
    }

    @Test
    void cancelacionDeSuscripcionDevuelveSalonAPlanGratis() throws Exception {
        Salon salon = salonConPlanYCreditos(Plan.PRO, 300);
        salon.setSuscripcionStripeId("sub-cancelada");
        Subscription subscription = mock(Subscription.class);
        when(subscription.getId()).thenReturn("sub-cancelada");
        Event evento = mock(Event.class);
        EventDataObjectDeserializer deserializador = mock(EventDataObjectDeserializer.class);
        when(evento.getId()).thenReturn("evt-cancelacion");
        when(evento.getType()).thenReturn("customer.subscription.deleted");
        when(evento.getDataObjectDeserializer()).thenReturn(deserializador);
        when(deserializador.getObject()).thenReturn(Optional.of(subscription));
        when(eventoStripeRepository.existsById("evt-cancelacion")).thenReturn(false);
        when(salonRepository.findBySuscripcionStripeId("sub-cancelada")).thenReturn(Optional.of(salon));

        try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
            webhook.when(() -> Webhook.constructEvent(PAYLOAD, FIRMA, SECRETO)).thenReturn(evento);

            webhookService.procesarEvento(PAYLOAD, FIRMA);
        }

        assertEquals(Plan.GRATIS, salon.getPlan());
        assertEquals(300, salon.getCreditosIa());
        assertEquals(null, salon.getSuscripcionStripeId());
        verify(salonRepository).save(salon);
        verify(eventoStripeRepository).save(any(EventoStripe.class));
    }

    @Test
    void firmaInvalidaSeRechazaSinModificarDatos() {
        SignatureVerificationException error = new SignatureVerificationException("firma falsa", FIRMA);

        try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
            webhook.when(() -> Webhook.constructEvent(PAYLOAD, FIRMA, SECRETO)).thenThrow(error);

            assertThrows(FirmaStripeInvalidaException.class,
                    () -> webhookService.procesarEvento(PAYLOAD, FIRMA));
        }

        verify(salonRepository, never()).save(any(Salon.class));
        verify(eventoStripeRepository, never()).save(any(EventoStripe.class));
    }

    private Salon salonConPlanYCreditos(Plan plan, int creditos) {
        Salon salon = new Salon();
        salon.setId(55L);
        salon.setPlan(plan);
        salon.setCreditosIa(creditos);
        return salon;
    }

    private Session mockSession(String tipo, String suscripcionId) {
        Session session = mock(Session.class);
        when(session.getPaymentStatus()).thenReturn("paid");
        when(session.getMetadata()).thenReturn(Map.of("salonId", "55", "tipo", tipo));
        if (suscripcionId != null) {
            when(session.getSubscription()).thenReturn(suscripcionId);
        }
        return session;
    }

    private Event eventoPago(String id, Session session) {
        Event evento = mock(Event.class);
        EventDataObjectDeserializer deserializador = mock(EventDataObjectDeserializer.class);
        when(evento.getId()).thenReturn(id);
        when(evento.getType()).thenReturn("checkout.session.completed");
        when(evento.getDataObjectDeserializer()).thenReturn(deserializador);
        when(deserializador.getObject()).thenReturn(Optional.<StripeObject>of(session));
        return evento;
    }
}
