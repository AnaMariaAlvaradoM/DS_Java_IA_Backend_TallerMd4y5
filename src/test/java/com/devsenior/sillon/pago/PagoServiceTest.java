package com.devsenior.sillon.pago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsenior.sillon.compartido.FirmaInvalidaException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.stripe.StripeClient;

/**
 * PagoService.procesarWebhook llama al metodo estatico real
 * com.stripe.net.Webhook.constructEvent(cuerpo, firma, secreto), que no se puede
 * mockear sin tocar produccion ni agregar dependencias (no hay mockito-inline en
 * el proyecto). Por eso estos tests firman el cuerpo de prueba con HMAC-SHA256
 * exactamente como lo hace Stripe: "t=<timestamp>,v1=<hex(HMAC_SHA256(secreto,
 * timestamp + "." + cuerpo))>", usando un secreto de prueba configurado en
 * StripeProperties. El cuerpo es un JSON minimo pero real de un Event de Stripe.
 */
@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    private static final String SECRETO_WEBHOOK = "whsec_test_123";
    private static final Long SALON_ID = 1L;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-10T15:00:00Z"), ZoneId.of("America/Bogota"));

    @Mock
    private SalonRepository salonRepository;
    @Mock
    private EventoStripeRepository eventoStripeRepository;

    private PagoService pagoService;
    private Salon salon;

    @BeforeEach
    void setUp() {
        StripeProperties stripeProperties = new StripeProperties();
        stripeProperties.setSecretoWebhook(SECRETO_WEBHOOK);

        pagoService = new PagoService(salonRepository, eventoStripeRepository, mock(StripeClient.class),
                stripeProperties, CLOCK, "http://localhost:8080", 300, 200);

        salon = new Salon();
        salon.setId(SALON_ID);
        salon.setPlan(Plan.GRATIS);
    }

    private static String firmar(String secreto, String cuerpo) throws NoSuchAlgorithmException {
        try {
            long timestamp = Instant.now().getEpochSecond();
            String payload = timestamp + "." + cuerpo;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return "t=" + timestamp + ",v1=" + hex;
        } catch (java.security.InvalidKeyException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String eventoSesionCompletada(String eventId, String tipoCompra, boolean conSuscripcion) {
        String suscripcion = conSuscripcion ? "\"subscription\": \"sub_test_1\"," : "";
        return """
                {
                  "id": "%s",
                  "object": "event",
                  "api_version": "2020-08-27",
                  "created": 1700000000,
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_test_1",
                      "object": "checkout.session",
                      "payment_status": "paid",
                      %s
                      "metadata": {
                        "salonId": "%d",
                        "tipo": "%s"
                      }
                    }
                  }
                }
                """.formatted(eventId, suscripcion, SALON_ID, tipoCompra);
    }

    private static String eventoSuscripcionCancelada(String eventId, String suscripcionId) {
        return """
                {
                  "id": "%s",
                  "object": "event",
                  "api_version": "2020-08-27",
                  "created": 1700000000,
                  "type": "customer.subscription.deleted",
                  "data": {
                    "object": {
                      "id": "%s",
                      "object": "subscription"
                    }
                  }
                }
                """.formatted(eventId, suscripcionId);
    }

    @Test
    void pagoProActivaElPlanYSuma300Creditos() throws Exception {
        String cuerpo = eventoSesionCompletada("evt_pro_1", "SUSCRIPCION_PRO", true);
        String firma = firmar(SECRETO_WEBHOOK, cuerpo);
        when(eventoStripeRepository.existsById("evt_pro_1")).thenReturn(false);
        when(salonRepository.findById(SALON_ID)).thenReturn(Optional.of(salon));

        WebhookRespuesta respuesta = pagoService.procesarWebhook(cuerpo, firma);

        assertThat(respuesta.procesado()).isTrue();
        assertThat(salon.getPlan()).isEqualTo(Plan.PRO);
        assertThat(salon.getStripeSuscripcionId()).isEqualTo("sub_test_1");
        verify(salonRepository).sumarCreditos(SALON_ID, 300);
        verify(salonRepository).save(salon);
        verify(eventoStripeRepository).save(any());
    }

    @Test
    void elPaqueteDeCreditosSoloSuma200() throws Exception {
        String cuerpo = eventoSesionCompletada("evt_paquete_1", "PAQUETE_CREDITOS", false);
        String firma = firmar(SECRETO_WEBHOOK, cuerpo);
        when(eventoStripeRepository.existsById("evt_paquete_1")).thenReturn(false);

        WebhookRespuesta respuesta = pagoService.procesarWebhook(cuerpo, firma);

        assertThat(respuesta.procesado()).isTrue();
        verify(salonRepository).sumarCreditos(SALON_ID, 200);
        verify(salonRepository, never()).save(any());
    }

    @Test
    void unEventoRepetidoNoSeProcesaDeNuevo() throws Exception {
        String cuerpo = eventoSesionCompletada("evt_repetido_1", "SUSCRIPCION_PRO", true);
        String firma = firmar(SECRETO_WEBHOOK, cuerpo);
        when(eventoStripeRepository.existsById("evt_repetido_1")).thenReturn(true);

        WebhookRespuesta respuesta = pagoService.procesarWebhook(cuerpo, firma);

        assertThat(respuesta).isEqualTo(new WebhookRespuesta(true, false));
        verify(salonRepository, never()).sumarCreditos(any(), anyInt());
        verify(salonRepository, never()).save(any());
        verify(eventoStripeRepository, never()).save(any());
    }

    @Test
    void laCancelacionVuelveAGratisYConservaCreditos() throws Exception {
        salon.setPlan(Plan.PRO);
        salon.setCreditosIa(150);
        salon.setStripeSuscripcionId("sub_test_1");

        String cuerpo = eventoSuscripcionCancelada("evt_cancel_1", "sub_test_1");
        String firma = firmar(SECRETO_WEBHOOK, cuerpo);
        when(eventoStripeRepository.existsById("evt_cancel_1")).thenReturn(false);
        when(salonRepository.findByStripeSuscripcionId("sub_test_1")).thenReturn(Optional.of(salon));

        WebhookRespuesta respuesta = pagoService.procesarWebhook(cuerpo, firma);

        assertThat(respuesta.procesado()).isTrue();
        assertThat(salon.getPlan()).isEqualTo(Plan.GRATIS);
        assertThat(salon.getCreditosIa()).isEqualTo(150);
        verify(salonRepository, never()).sumarCreditos(any(), anyInt());
        verify(salonRepository).save(salon);
    }

    @Test
    void sinFirmaDaFirmaInvalida() {
        assertThatThrownBy(() -> pagoService.procesarWebhook("{}", null))
                .isInstanceOf(FirmaInvalidaException.class);

        verifyNoInteractions(eventoStripeRepository);
        verifyNoInteractions(salonRepository);
    }

    @Test
    void unaFirmaFalsaDaFirmaInvalida() throws Exception {
        String cuerpo = eventoSesionCompletada("evt_falso_1", "SUSCRIPCION_PRO", true);
        String firmaConSecretoIncorrecto = firmar("whsec_otro_secreto", cuerpo);

        assertThatThrownBy(() -> pagoService.procesarWebhook(cuerpo, firmaConSecretoIncorrecto))
                .isInstanceOf(FirmaInvalidaException.class);

        verifyNoInteractions(eventoStripeRepository);
        verifyNoInteractions(salonRepository);
    }
}
