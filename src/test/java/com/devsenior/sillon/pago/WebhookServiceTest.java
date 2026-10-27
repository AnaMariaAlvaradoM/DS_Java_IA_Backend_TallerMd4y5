package com.devsenior.sillon.pago;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.devsenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devsenior.sillon.compartido.excepcion.PasarelaException;
import com.devsenior.sillon.salon.*;
import com.stripe.Stripe;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

class WebhookServiceTest {
    private final Clock reloj = Clock.fixed(Instant.parse("2026-10-20T15:00:00Z"), ZoneId.of("America/Bogota"));
    private final EventoStripeRepository eventos = mock(EventoStripeRepository.class);
    private final SalonRepository salones = mock(SalonRepository.class);
    private final Salon salon = new Salon();
    private final WebhookService servicio;

    WebhookServiceTest() {
        StripeProperties config = new StripeProperties(); config.setSecretoWebhook("whsec_ficticio_test");
        servicio = new WebhookService(new VerificadorWebhook(config, reloj),
                new ProcesadorEventoStripe(eventos, salones, reloj, 300, 200), eventos);
        salon.setId(1L); salon.setPlan(Plan.GRATIS); salon.setCreditosIa(10);
        when(salones.findByIdParaActualizar(1L)).thenReturn(Optional.of(salon));
    }

    @ParameterizedTest @ValueSource(strings = {"tipo", "tipoCompra", "ambas"})
    void pagoProActivaPlanSumaCreditosYEventoRepetidoNoSeAplica(String clave) throws Exception {
        String cuerpo = compra("SUSCRIPCION_PRO", clave);
        when(eventos.existsById("evt_prueba")).thenReturn(false, true);
        assertTrue(servicio.recibir(cuerpo, firmar(cuerpo)).procesado());
        assertEquals(Plan.PRO, salon.getPlan()); assertEquals(310, salon.getCreditosIa());
        assertEquals("sub_prueba", salon.getStripeSuscripcionId());
        assertFalse(servicio.recibir(cuerpo, firmar(cuerpo)).procesado());
        assertEquals(310, salon.getCreditosIa());
        verify(salones, times(1)).save(salon); verify(eventos, times(1)).saveAndFlush(any());
    }

    @ParameterizedTest @ValueSource(strings = {"tipo", "tipoCompra", "ambas"})
    void paqueteSoloSumaCreditos(String clave) throws Exception {
        String cuerpo = compra("PAQUETE_CREDITOS", clave);
        servicio.recibir(cuerpo, firmar(cuerpo));
        assertEquals(Plan.GRATIS, salon.getPlan()); assertEquals(210, salon.getCreditosIa());
        verify(salones).save(salon);
    }

    @Test void cancelacionVuelveAGratis() throws Exception {
        salon.setPlan(Plan.PRO); salon.setCreditosIa(310); salon.setStripeSuscripcionId("sub_prueba");
        when(salones.findByStripeSuscripcionId("sub_prueba")).thenReturn(Optional.of(salon));
        String cuerpo = evento("customer.subscription.deleted", Map.of("id", "sub_prueba", "object", "subscription"));
        servicio.recibir(cuerpo, firmar(cuerpo));
        assertEquals(Plan.GRATIS, salon.getPlan()); assertEquals(310, salon.getCreditosIa());
        verify(salones).save(salon);
    }

    @Test void firmaFalsaEs400YNoProcesa() {
        var error = assertThrows(FirmaInvalidaException.class, () -> servicio.recibir("{}", "t=1,v1=falsa"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getEstado());
        verifyNoInteractions(eventos); verify(salones, never()).save(any());
    }

    @Test void metadataConTiposDistintosNoCambiaPlanNiRegistraEvento() throws Exception {
        String cuerpo = evento("checkout.session.completed", Map.of("id", "cs_test_prueba", "object", "checkout.session",
                "payment_status", "paid", "subscription", "sub_prueba", "metadata",
                Map.of("salonId", "1", "tipo", "SUSCRIPCION_PRO", "tipoCompra", "PAQUETE_CREDITOS")));
        var error = assertThrows(PasarelaException.class, () -> servicio.recibir(cuerpo, firmar(cuerpo)));
        assertEquals(HttpStatus.BAD_GATEWAY, error.getEstado());
        assertEquals(Plan.GRATIS, salon.getPlan()); assertEquals(10, salon.getCreditosIa());
        verify(salones, never()).save(any()); verify(eventos, never()).saveAndFlush(any());
    }

    private String compra(String tipo, String clave) throws Exception {
        Map<String, String> metadata = clave.equals("ambas")
                ? Map.of("salonId", "1", "tipo", tipo, "tipoCompra", tipo)
                : Map.of("salonId", "1", clave, tipo);
        return evento("checkout.session.completed", Map.of("id", "cs_test_prueba", "object", "checkout.session",
                "payment_status", "paid", "subscription", "sub_prueba", "metadata", metadata));
    }
    private String evento(String tipo, Map<String, ?> objeto) throws Exception {
        return JsonMapper.builder().build().writeValueAsString(Map.of("id", "evt_prueba", "object", "event", "type", tipo,
                "api_version", Stripe.API_VERSION, "data", Map.of("object", objeto)));
    }
    private String firmar(String cuerpo) throws Exception {
        long tiempo = reloj.instant().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("whsec_ficticio_test".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "t=" + tiempo + ",v1=" + HexFormat.of().formatHex(mac.doFinal((tiempo + "." + cuerpo).getBytes(StandardCharsets.UTF_8)));
    }
}
