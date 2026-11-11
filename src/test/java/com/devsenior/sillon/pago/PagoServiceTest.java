package com.devsenior.sillon.pago;

import com.devsenior.sillon.compartido.FirmaInvalidaException;
import com.devsenior.sillon.pago.dto.WebhookRespuestaDto;
import com.devsenior.sillon.salon.SalonRepository;
import com.stripe.StripeClient;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    private static final String SECRETO = "whsec_test_secret";

    @Mock
    private SalonRepository salonRepository;

    @Mock
    private EventoStripeRepository eventoStripeRepository;

    @Mock
    private StripeProperties stripeProperties;

    @Mock
    private StripeClient stripeClient;

    private PagoService pagoService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-09T15:00:00Z"), ZoneId.of("America/Bogota"));
        pagoService = new PagoService(
                salonRepository,
                eventoStripeRepository,
                stripeProperties,
                stripeClient,
                clock
        );
    }

    @Test
    @DisplayName("1. Firma nula o vacía: procesarWebhook debe lanzar FirmaInvalidaException")
    void firmaNulaOVaciaLanzaExcepcion() {
        String payload = "{\"id\":\"evt_123\"}";

        assertThrows(FirmaInvalidaException.class, () -> pagoService.procesarWebhook(payload, null));
        assertThrows(FirmaInvalidaException.class, () -> pagoService.procesarWebhook(payload, "   "));

        verify(eventoStripeRepository, never()).existsById(anyString());
        verify(salonRepository, never()).save(any());
    }

    @Test
    @DisplayName("2. Firma inválida: procesarWebhook lanza FirmaInvalidaException")
    void firmaInvalidaLanzaExcepcion() {
        String payload = "{\"id\":\"evt_123\"}";
        String firmaInvalida = "t=1700000000,v1=firma_falsa_invalid";

        when(stripeProperties.getSecretoWebhook()).thenReturn(SECRETO);

        assertThrows(FirmaInvalidaException.class, () -> pagoService.procesarWebhook(payload, firmaInvalida));

        verify(eventoStripeRepository, never()).existsById(anyString());
        verify(salonRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. Evento duplicado (idempotencia): no vuelve a procesar ni guardar")
    void eventoDuplicadoRetornaIdempotente() {
        String payload = "{\"id\":\"evt_dup_999\",\"object\":\"event\",\"type\":\"checkout.session.completed\","
                + "\"data\":{\"object\":{\"id\":\"cs_1\",\"object\":\"checkout.session\"}}}";
        long timestamp = Instant.now().getEpochSecond();
        String firma = generarFirmaStripe(payload, SECRETO, timestamp);

        when(stripeProperties.getSecretoWebhook()).thenReturn(SECRETO);
        when(eventoStripeRepository.existsById("evt_dup_999")).thenReturn(true);

        WebhookRespuestaDto respuesta = pagoService.procesarWebhook(payload, firma);

        assertNotNull(respuesta);
        assertTrue(respuesta.recibido());
        assertFalse(respuesta.procesado());

        verify(eventoStripeRepository, never()).save(any());
        verify(salonRepository, never()).save(any());
    }

    @Test
    @DisplayName("4. Evento de tipo no manejado: se registra el evento sin alterar el salón")
    void eventoIgnorableSeRegistraEnBd() {
        String apiVersion = com.stripe.Stripe.API_VERSION;
        String payload = "{\"id\":\"evt_otro_456\",\"object\":\"event\",\"api_version\":\"2020-08-27\","
                + "\"type\":\"customer.created\","
                + "\"data\":{\"object\":{\"id\":\"cus_1\",\"object\":\"customer\"}}}";
        long timestamp = Instant.now().getEpochSecond();
        String firma = generarFirmaStripe(payload, SECRETO, timestamp);

        when(stripeProperties.getSecretoWebhook()).thenReturn(SECRETO);
        when(eventoStripeRepository.existsById("evt_otro_456")).thenReturn(false);

        WebhookRespuestaDto respuesta = pagoService.procesarWebhook(payload, firma);

        assertNotNull(respuesta);
        assertTrue(respuesta.recibido());
        assertFalse(respuesta.procesado());

        verify(eventoStripeRepository).save(any(EventoStripe.class));
        verify(salonRepository, never()).save(any());
    }

    private String generarFirmaStripe(String payload, String secreto, long timestamp) {
        try {
            String hash = Webhook.Util.computeHmacSha256(secreto, timestamp + "." + payload);
            return "t=" + timestamp + ",v1=" + hash;
        } catch (Exception e) {
            throw new RuntimeException("Error al calcular la firma de Stripe para el test", e);
        }
    }
}