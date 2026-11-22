package com.devsenior.sillon.pago;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.devsenior.sillon.compartido.FirmaInvalidaException;
import com.devsenior.sillon.compartido.NegocioConfig;
import com.devsenior.sillon.compartido.PasarelaException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.stripe.StripeClient;
import com.stripe.exception.ApiException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.service.CheckoutService;
import com.stripe.service.checkout.SessionService;

class PagoServicioTest {

    @Mock
    private StripeClient stripeClient;

    @Mock
    private SalonRepository salonRepository;

    @Mock
    private EventoStripeRepository eventoStripeRepository;

    private StripeConfig stripeConfig;
    private NegocioConfig negocioConfig;
    private Clock reloj;
    private PagoServicio pagoServicio;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        stripeConfig = new StripeConfig("sk_test_123", "whsec_test_secret", "price_pro_123", "price_creditos_456", "https://api.stripe.com");
        negocioConfig = new NegocioConfig(10, 300, 200, 2);
        reloj = Clock.fixed(Instant.parse("2026-10-20T10:00:00Z"), ZoneId.of("America/Bogota"));

        pagoServicio = new PagoServicio(
                stripeClient,
                stripeConfig,
                negocioConfig,
                salonRepository,
                eventoStripeRepository,
                reloj,
                "http://localhost:8080"
        );
    }

    @Test
    @DisplayName("Crear sesión de suscripción cuando el salón ya es PRO responde 409 antes de llamar a Stripe")
    void crearSesionSuscripcion_salonYaEnPlanPro_lanza409() {
        Salon salon = new Salon();
        salon.setPlan(Plan.PRO);
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> pagoServicio.crearSesionSuscripcion(1L, "dueno@test.com"));

        assertTrue(ex.getMessage().contains("ya cuenta con el plan PRO"));
        verify(stripeClient, never()).checkout();
    }

    @Test
    @DisplayName("Crear sesión de suscripción para salón GRATIS llama a Stripe y devuelve datos de sesión")
    void crearSesionSuscripcion_salonGratis_exito() throws Exception {
        Salon salon = new Salon();
        salon.setId(1L);
        salon.setPlan(Plan.GRATIS);
        salon.setNombre("Barbería Central");
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));

        CheckoutService checkoutService = mock(CheckoutService.class);
        SessionService sessionService = mock(SessionService.class);
        Session stripeSession = mock(Session.class);

        when(stripeClient.checkout()).thenReturn(checkoutService);
        when(checkoutService.sessions()).thenReturn(sessionService);
        when(stripeSession.getId()).thenReturn("cs_test_sub_123");
        when(stripeSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_test_sub_123");
        when(sessionService.create(any(SessionCreateParams.class))).thenReturn(stripeSession);

        SesionPagoRespuesta respuesta = pagoServicio.crearSesionSuscripcion(1L, "dueno@test.com");

        assertEquals(TipoCompra.SUSCRIPCION_PRO, respuesta.tipo());
        assertEquals("cs_test_sub_123", respuesta.sesionId());
        assertTrue(respuesta.urlPago().startsWith("https://"));
    }

    @Test
    @DisplayName("Crear sesión de suscripción cuando Stripe lanza excepción lanza PasarelaException (502)")
    void crearSesionSuscripcion_errorStripe_lanza502() throws Exception {
        Salon salon = new Salon();
        salon.setId(1L);
        salon.setPlan(Plan.GRATIS);
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));

        CheckoutService checkoutService = mock(CheckoutService.class);
        SessionService sessionService = mock(SessionService.class);

        when(stripeClient.checkout()).thenReturn(checkoutService);
        when(checkoutService.sessions()).thenReturn(sessionService);
        when(sessionService.create(any(SessionCreateParams.class)))
                .thenThrow(new ApiException("Stripe caído", null, null, 500, null));

        PasarelaException ex = assertThrows(PasarelaException.class,
                () -> pagoServicio.crearSesionSuscripcion(1L, "dueno@test.com"));

        assertTrue(ex.getMessage().contains("Stripe"));
    }

    @Test
    @DisplayName("Crear sesión de compra de créditos llama a Stripe y devuelve tipo PAQUETE_CREDITOS")
    void crearSesionCreditos_exito() throws Exception {
        Salon salon = new Salon();
        salon.setId(1L);
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));

        CheckoutService checkoutService = mock(CheckoutService.class);
        SessionService sessionService = mock(SessionService.class);
        Session stripeSession = mock(Session.class);

        when(stripeClient.checkout()).thenReturn(checkoutService);
        when(checkoutService.sessions()).thenReturn(sessionService);
        when(stripeSession.getId()).thenReturn("cs_test_cred_456");
        when(stripeSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_test_cred_456");
        when(sessionService.create(any(SessionCreateParams.class))).thenReturn(stripeSession);

        SesionPagoRespuesta respuesta = pagoServicio.crearSesionCreditos(1L, "dueno@test.com");

        assertEquals(TipoCompra.PAQUETE_CREDITOS, respuesta.tipo());
        assertEquals("cs_test_cred_456", respuesta.sesionId());
        assertTrue(respuesta.urlPago().startsWith("https://"));
    }

    @Test
    @DisplayName("Webhook sin cabecera Stripe-Signature lanza FirmaInvalidaException (400)")
    void webhook_sinCabeceraFirma_lanza400() {
        assertThrows(FirmaInvalidaException.class,
                () -> pagoServicio.procesarWebhook("{}", null));
        assertThrows(FirmaInvalidaException.class,
                () -> pagoServicio.procesarWebhook("{}", "   "));
    }

    @Test
    @DisplayName("Webhook con firma falsa lanza FirmaInvalidaException (400)")
    void webhook_firmaFalsa_lanza400() {
        assertThrows(FirmaInvalidaException.class,
                () -> pagoServicio.procesarWebhook("{}", "t=12345,v1=firmafalsa123"));
    }

    @Test
    @DisplayName("Webhook con evento ya procesado devuelve procesado=false y no toca la base de datos")
    void webhook_eventoRepetido_noProcesa() {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn("evt_repetido_123");
        when(eventoStripeRepository.existsById("evt_repetido_123")).thenReturn(true);

        WebhookRespuesta respuesta = pagoServicio.procesarEvento(event);

        assertTrue(respuesta.recibido());
        assertFalse(respuesta.procesado());
        verify(salonRepository, never()).save(any());
        verify(eventoStripeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Webhook checkout.session.completed activa plan PRO, guarda suscripción y suma 300 créditos")
    void webhook_pagoPro_activaPlanYSumaCreditos() {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn("evt_pago_pro_1");
        when(event.getType()).thenReturn("checkout.session.completed");
        when(eventoStripeRepository.existsById("evt_pago_pro_1")).thenReturn(false);

        Session session = mock(Session.class);
        when(session.getPaymentStatus()).thenReturn("paid");
        Map<String, String> metadata = new HashMap<>();
        metadata.put("salonId", "1");
        metadata.put("tipoCompra", "SUSCRIPCION_PRO");
        when(session.getMetadata()).thenReturn(metadata);
        when(session.getSubscription()).thenReturn("sub_stripe_pro_999");

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(session));

        Salon salon = new Salon();
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(10);
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));

        WebhookRespuesta respuesta = pagoServicio.procesarEvento(event);

        assertTrue(respuesta.recibido());
        assertTrue(respuesta.procesado());
        assertEquals(Plan.PRO, salon.getPlan());
        assertEquals(310, salon.getCreditosIa()); // 10 iniciales + 300
        assertEquals("sub_stripe_pro_999", salon.getStripeSuscripcionId());
        verify(salonRepository).save(salon);
        verify(eventoStripeRepository).save(any(EventoStripe.class));
    }

    @Test
    @DisplayName("Webhook checkout.session.completed paquete de créditos suma 200 créditos y conserva plan")
    void webhook_paqueteCreditos_suma200Creditos() {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn("evt_pago_creditos_2");
        when(event.getType()).thenReturn("checkout.session.completed");
        when(eventoStripeRepository.existsById("evt_pago_creditos_2")).thenReturn(false);

        Session session = mock(Session.class);
        when(session.getPaymentStatus()).thenReturn("paid");
        Map<String, String> metadata = new HashMap<>();
        metadata.put("salonId", "1");
        metadata.put("tipoCompra", "PAQUETE_CREDITOS");
        when(session.getMetadata()).thenReturn(metadata);

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(session));

        Salon salon = new Salon();
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(15);
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));

        WebhookRespuesta respuesta = pagoServicio.procesarEvento(event);

        assertTrue(respuesta.recibido());
        assertTrue(respuesta.procesado());
        assertEquals(Plan.GRATIS, salon.getPlan());
        assertEquals(215, salon.getCreditosIa()); // 15 + 200
        verify(salonRepository).save(salon);
        verify(eventoStripeRepository).save(any(EventoStripe.class));
    }

    @Test
    @DisplayName("Webhook customer.subscription.deleted devuelve salón a GRATIS y conserva sus créditos")
    void webhook_suscripcionCancelada_vuelveAGratis() {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn("evt_cancelacion_3");
        when(event.getType()).thenReturn("customer.subscription.deleted");
        when(eventoStripeRepository.existsById("evt_cancelacion_3")).thenReturn(false);

        Subscription subscription = mock(Subscription.class);
        when(subscription.getId()).thenReturn("sub_stripe_pro_999");

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(subscription));

        Salon salon = new Salon();
        salon.setPlan(Plan.PRO);
        salon.setCreditosIa(350);
        salon.setStripeSuscripcionId("sub_stripe_pro_999");
        when(salonRepository.findByStripeSuscripcionId("sub_stripe_pro_999")).thenReturn(Optional.of(salon));

        WebhookRespuesta respuesta = pagoServicio.procesarEvento(event);

        assertTrue(respuesta.recibido());
        assertTrue(respuesta.procesado());
        assertEquals(Plan.GRATIS, salon.getPlan());
        assertNull(salon.getStripeSuscripcionId());
        assertEquals(350, salon.getCreditosIa()); // Conserva los créditos adquiridos
        verify(salonRepository).save(salon);
        verify(eventoStripeRepository).save(any(EventoStripe.class));
    }

    @Test
    @DisplayName("Página de resultado de pago devuelve mensajes según estado")
    void obtenerResultado_mensajes() {
        ResultadoPagoRespuesta exito = pagoServicio.obtenerResultado("exito");
        assertTrue(exito.mensaje().contains("éxito"));

        ResultadoPagoRespuesta cancelado = pagoServicio.obtenerResultado("cancelado");
        assertTrue(cancelado.mensaje().contains("cancelado"));

        ResultadoPagoRespuesta otro = pagoServicio.obtenerResultado("otro");
        assertNotNull(otro.mensaje());
    }
}
