package com.devsenior.sillon.pago.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.devsenior.sillon.compartido.config.NegocioProperties;
import com.devsenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devsenior.sillon.pago.config.StripeProperties;
import com.devsenior.sillon.pago.dto.WebhookResponse;
import com.devsenior.sillon.pago.model.EventoStripe;
import com.devsenior.sillon.pago.repository.EventoStripeRepository;
import com.devsenior.sillon.salon.model.Plan;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.repository.SalonRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

/** Los eventos se firman igual que Stripe (y el calificador): HMAC-SHA256 de "timestamp.cuerpo" con el secreto. */
@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    private static final String SECRETO = "whsec_secreto_de_prueba";

    @Mock
    private EventoStripeRepository eventoStripeRepository;
    @Mock
    private SalonRepository salonRepository;

    private WebhookService webhookService;
    private Salon salon;

    @BeforeEach
    void setUp() {
        StripeProperties stripe = new StripeProperties("http://localhost:12111", "sk_test_x", SECRETO, "price_pro", "price_creditos");
        NegocioProperties negocio = new NegocioProperties(10, 300, 200, 2);
        webhookService = new WebhookService(eventoStripeRepository, salonRepository, JsonMapper.builder().build(),
                stripe, negocio, Clock.systemUTC());
        salon = new Salon();
        salon.setId(7L);
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(10);
    }

    @Test
    void procesar_pagoDelPlanPro_activaPlanGuardaSuscripcionYSuma300() {
        String cuerpo = pagoCompletado("evt_pro_1", "SUSCRIPCION_PRO", "\"sub_123\"");
        when(eventoStripeRepository.existsById("evt_pro_1")).thenReturn(false);
        when(salonRepository.findById(7L)).thenReturn(Optional.of(salon));

        WebhookResponse respuesta = webhookService.procesar(cuerpo, firmar(cuerpo, SECRETO));

        assertTrue(respuesta.procesado());
        assertEquals(Plan.PRO, salon.getPlan());
        assertEquals("sub_123", salon.getStripeSuscripcionId());
        assertEquals(310, salon.getCreditosIa());
        verify(eventoStripeRepository).save(any(EventoStripe.class));
    }

    @Test
    void procesar_paqueteDeCreditos_soloSuma200() {
        String cuerpo = pagoCompletado("evt_paquete_1", "PAQUETE_CREDITOS", "null");
        when(eventoStripeRepository.existsById("evt_paquete_1")).thenReturn(false);
        when(salonRepository.findById(7L)).thenReturn(Optional.of(salon));

        webhookService.procesar(cuerpo, firmar(cuerpo, SECRETO));

        assertEquals(Plan.GRATIS, salon.getPlan());
        assertEquals(210, salon.getCreditosIa());
    }

    @Test
    void procesar_eventoRepetido_noSeAplicaDosVeces() {
        String cuerpo = pagoCompletado("evt_pro_1", "SUSCRIPCION_PRO", "\"sub_123\"");
        when(eventoStripeRepository.existsById("evt_pro_1")).thenReturn(true);

        WebhookResponse respuesta = webhookService.procesar(cuerpo, firmar(cuerpo, SECRETO));

        assertTrue(respuesta.recibido());
        assertFalse(respuesta.procesado());
        verify(salonRepository, never()).save(any());
        verify(eventoStripeRepository, never()).save(any());
    }

    @Test
    void procesar_suscripcionCancelada_vuelveAGratisYConservaCreditos() {
        salon.setPlan(Plan.PRO);
        salon.setStripeSuscripcionId("sub_123");
        salon.setCreditosIa(510);
        String cuerpo = "{\"id\":\"evt_cancela_1\",\"object\":\"event\",\"api_version\":\"2026-09-30.endive\","
                + "\"type\":\"customer.subscription.deleted\","
                + "\"data\":{\"object\":{\"id\":\"sub_123\",\"object\":\"subscription\",\"status\":\"canceled\"}}}";
        when(eventoStripeRepository.existsById("evt_cancela_1")).thenReturn(false);
        when(salonRepository.findByStripeSuscripcionId("sub_123")).thenReturn(Optional.of(salon));

        webhookService.procesar(cuerpo, firmar(cuerpo, SECRETO));

        assertEquals(Plan.GRATIS, salon.getPlan());
        assertEquals(510, salon.getCreditosIa());
    }

    @Test
    void procesar_firmaFalsa_lanza400SinTocarNada() {
        String cuerpo = pagoCompletado("evt_pro_1", "SUSCRIPCION_PRO", "\"sub_123\"");

        assertThrows(FirmaInvalidaException.class,
                () -> webhookService.procesar(cuerpo, firmar(cuerpo, "whsec_esta_no_es_la_firma")));
        verifyNoInteractions(eventoStripeRepository, salonRepository);
    }

    @Test
    void procesar_sinFirma_lanza400() {
        String cuerpo = pagoCompletado("evt_pro_1", "SUSCRIPCION_PRO", "\"sub_123\"");

        assertThrows(FirmaInvalidaException.class, () -> webhookService.procesar(cuerpo, null));
        verifyNoInteractions(eventoStripeRepository, salonRepository);
    }

    @Test
    void procesar_cuerpoAlteradoDespuesDeFirmar_lanza400() {
        String original = pagoCompletado("evt_pro_1", "SUSCRIPCION_PRO", "\"sub_123\"");
        String firma = firmar(original, SECRETO);
        String alterado = original.replace("SUSCRIPCION_PRO", "PAQUETE_CREDITOS");

        assertThrows(FirmaInvalidaException.class, () -> webhookService.procesar(alterado, firma));
    }

    private String pagoCompletado(String id, String tipo, String suscripcionJson) {
        return "{\"id\":\"" + id + "\",\"object\":\"event\",\"api_version\":\"2026-09-30.endive\","
                + "\"type\":\"checkout.session.completed\",\"data\":{\"object\":{\"id\":\"cs_test_1\","
                + "\"object\":\"checkout.session\",\"payment_status\":\"paid\",\"subscription\":" + suscripcionJson + ","
                + "\"metadata\":{\"salonId\":\"7\",\"tipo\":\"" + tipo + "\"}}}}";
    }

    private String firmar(String cuerpo, String secreto) {
        try {
            long t = System.currentTimeMillis() / 1000;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String v1 = HexFormat.of().formatHex(mac.doFinal((t + "." + cuerpo).getBytes(StandardCharsets.UTF_8)));
            return "t=" + t + ",v1=" + v1;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
