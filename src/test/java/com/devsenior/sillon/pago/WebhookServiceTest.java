package com.devsenior.sillon.pago;

import com.devsenior.sillon.compartido.FirmaInvalidaException;
import com.devsenior.sillon.compartido.PasarelaException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebhookServiceTest {
    private static final String SECRETO = "whsec_prueba_local";
    private EventoStripeRepository eventos;
    private SalonRepository salones;
    private WebhookService servicio;

    @BeforeEach
    void preparar() {
        eventos = mock(EventoStripeRepository.class);
        salones = mock(SalonRepository.class);
        Clock reloj = Clock.fixed(Instant.parse("2026-11-20T12:00:00Z"), ZoneOffset.UTC);
        servicio = new WebhookService(eventos, salones, SECRETO, 300, 200, reloj);
    }

    @Test
    void rechazaFirmaAusente() {
        assertThrows(FirmaInvalidaException.class, () -> servicio.procesar(evento("evt_1", "ping", "{}"), null));
        verifyNoInteractions(eventos, salones);
    }

    @Test
    void rechazaFirmaIncorrecta() {
        assertThrows(FirmaInvalidaException.class,
                () -> servicio.procesar(evento("evt_1", "ping", "{}"), "t=1,v1=incorrecta"));
        verifyNoInteractions(eventos, salones);
    }

    @Test
    void registraEventoNoReconocidoSinModificarSalon() {
        WebhookRespuesta respuesta = enviar(evento("evt_ping", "ping", "{}"));
        assertTrue(respuesta.recibido());
        assertFalse(respuesta.procesado());
        verify(eventos).save(any(EventoStripe.class));
        verifyNoInteractions(salones);
    }

    @Test
    void noProcesaDosVecesElMismoId() {
        when(eventos.existsById("evt_duplicado")).thenReturn(true);
        WebhookRespuesta respuesta = enviar(evento("evt_duplicado", "ping", "{}"));
        assertTrue(respuesta.recibido());
        assertFalse(respuesta.procesado());
        verify(eventos, never()).save(any());
    }

    @Test
    void noAcreditaCheckoutSinPagoConfirmado() {
        String cuerpo = evento("evt_pendiente", "checkout.session.completed", checkout("unpaid", "PAQUETE_CREDITOS", null));
        WebhookRespuesta respuesta = enviar(cuerpo);
        assertFalse(respuesta.procesado());
        verify(eventos).save(any());
        verifyNoInteractions(salones);
    }

    @Test
    void acreditaDoscientosCreditosPorPaquetePagado() {
        Salon salon = salon(10, Plan.GRATIS);
        when(salones.findById(7L)).thenReturn(Optional.of(salon));
        WebhookRespuesta respuesta = enviar(evento("evt_paquete", "checkout.session.completed",
                checkout("paid", "PAQUETE_CREDITOS", null)));
        assertTrue(respuesta.procesado());
        assertEquals(210, salon.getCreditosIa());
        assertEquals(Plan.GRATIS, salon.getPlan());
        verify(salones).save(salon);
    }

    @Test
    void activaProYAgregaTrescientosCreditos() {
        Salon salon = salon(10, Plan.GRATIS);
        when(salones.findById(7L)).thenReturn(Optional.of(salon));
        WebhookRespuesta respuesta = enviar(evento("evt_pro", "checkout.session.completed",
                checkout("paid", "SUSCRIPCION_PRO", "sub_123")));
        assertTrue(respuesta.procesado());
        assertEquals(310, salon.getCreditosIa());
        assertEquals(Plan.PRO, salon.getPlan());
        assertEquals("sub_123", salon.getStripeSuscripcionId());
    }

    @Test
    void cancelacionConservaLosCreditos() {
        Salon salon = salon(310, Plan.PRO);
        salon.setStripeSuscripcionId("sub_123");
        when(salones.findByStripeSuscripcionId("sub_123")).thenReturn(Optional.of(salon));
        WebhookRespuesta respuesta = enviar(evento("evt_cancelada", "customer.subscription.deleted",
                "{\"id\":\"sub_123\",\"object\":\"subscription\"}"));
        assertTrue(respuesta.procesado());
        assertEquals(Plan.GRATIS, salon.getPlan());
        assertNull(salon.getStripeSuscripcionId());
        assertEquals(310, salon.getCreditosIa());
    }

    @Test
    void cancelacionDesconocidaNoCambiaSalones() {
        WebhookRespuesta respuesta = enviar(evento("evt_ajena", "customer.subscription.deleted",
                "{\"id\":\"sub_ajena\",\"object\":\"subscription\"}"));
        assertFalse(respuesta.procesado());
        verify(salones, never()).save(any());
    }

    @Test
    void rechazaCompraSinTipoValido() {
        Salon salon = salon(10, Plan.GRATIS);
        when(salones.findById(7L)).thenReturn(Optional.of(salon));
        assertThrows(PasarelaException.class, () -> enviar(evento("evt_invalido", "checkout.session.completed",
                checkout("paid", "OTRO", null))));
        assertEquals(10, salon.getCreditosIa());
        verify(eventos, never()).save(any());
    }

    private Salon salon(int creditos, Plan plan) {
        Salon salon = new Salon();
        salon.setId(7L);
        salon.setCreditosIa(creditos);
        salon.setPlan(plan);
        return salon;
    }

    private String checkout(String estadoPago, String tipo, String suscripcion) {
        String sub = suscripcion == null ? "null" : "\"" + suscripcion + "\"";
        return "{\"id\":\"cs_test_123\",\"object\":\"checkout.session\",\"payment_status\":\""
                + estadoPago + "\",\"subscription\":" + sub
                + ",\"metadata\":{\"salonId\":\"7\",\"tipo\":\"" + tipo + "\"}}";
    }

    private String evento(String id, String tipo, String objeto) {
        return "{\"id\":\"" + id + "\",\"object\":\"event\",\"api_version\":\"2026-10-29.clover\","
                + "\"type\":\"" + tipo + "\",\"data\":{\"object\":" + objeto + "}}";
    }

    private WebhookRespuesta enviar(String cuerpo) {
        return servicio.procesar(cuerpo, firma(cuerpo));
    }

    private String firma(String cuerpo) {
        try {
            long instante = Instant.now().getEpochSecond();
            String contenido = instante + "." + cuerpo;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "t=" + instante + ",v1=" + HexFormat.of().formatHex(mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
