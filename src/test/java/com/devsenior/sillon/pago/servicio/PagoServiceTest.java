package com.devsenior.sillon.pago.servicio;

import com.devsenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.pago.dto.WebhookResponse;
import com.devsenior.sillon.pago.modelo.TipoCompra;
import com.devsenior.sillon.pago.repositorio.EventoStripeRepository;
import com.devsenior.sillon.salon.modelo.Plan;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepository;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.usuario.modelo.Rol;
import com.devsenior.sillon.usuario.modelo.Usuario;
import com.stripe.StripeClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock private StripeClient stripeClient;
    @Mock private SalonRepository salonRepository;
    @Mock private EventoStripeRepository eventoStripeRepository;

    private PagoService pagoService;
    private final String secretoWebhook = "whsec_test_secret_123456";
    private final Clock clock = Clock.system(ZoneId.of("America/Bogota"));

    @BeforeEach
    void setUp() {
        pagoService = new PagoService(
                stripeClient, salonRepository, eventoStripeRepository, clock,
                secretoWebhook, "price_pro", "price_cred", "http://localhost:8080", 300, 200
        );
    }

    private UsuarioPrincipal crearPrincipal(Salon salon) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setNombre("Dueño");
        u.setEmail("dueno@salon.com");
        u.setPassword("hash");
        u.setRol(Rol.DUENO);
        u.setActivo(true);
        u.setSalon(salon);
        return new UsuarioPrincipal(u);
    }

    private String firmarPayload(String payload, long timestamp) throws Exception {
        String signed = timestamp + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretoWebhook.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(signed.getBytes(StandardCharsets.UTF_8));
        return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(hash);
    }

    @Test
    void crearSesionPago_salonYaEsPro_lanza409() {
        Salon salon = new Salon();
        salon.setId(1L);
        salon.setPlan(Plan.PRO);
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));

        assertThrows(ReglaNegocioException.class,
                () -> pagoService.crearSesionPago(crearPrincipal(salon), TipoCompra.SUSCRIPCION_PRO));
        verifyNoInteractions(stripeClient);
    }

    @Test
    void crearSesionPago_salonInexistente_lanza404() {
        Salon salon = new Salon();
        salon.setId(99L);
        when(salonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> pagoService.crearSesionPago(crearPrincipal(salon), TipoCompra.PAQUETE_CREDITOS));
    }

    @Test
    void procesarWebhook_sinFirma_lanza400() {
        assertThrows(FirmaInvalidaException.class, () -> pagoService.procesarWebhook("{}", null));
        assertThrows(FirmaInvalidaException.class, () -> pagoService.procesarWebhook("{}", "  "));
    }

    @Test
    void procesarWebhook_firmaInvalida_lanza400() {
        assertThrows(FirmaInvalidaException.class,
                () -> pagoService.procesarWebhook("{\"id\":\"evt_1\"}", "t=123,v1=firma_falsa"));
    }

    @Test
    void procesarWebhook_eventoRepetido_retornaProcesadoFalse() throws Exception {
        String payload = "{\"id\":\"evt_repetido\",\"object\":\"event\",\"type\":\"checkout.session.completed\"}";
        String firma = firmarPayload(payload, Instant.now(clock).getEpochSecond());
        when(eventoStripeRepository.existsById("evt_repetido")).thenReturn(true);

        WebhookResponse resp = pagoService.procesarWebhook(payload, firma);

        assertTrue(resp.recibido());
        assertFalse(resp.procesado());
        verify(salonRepository, never()).save(any());
    }

    @Test
    void procesarWebhook_checkoutProPagado_actualizaPlanYSumaCreditos() throws Exception {
        Salon salon = new Salon();
        salon.setId(1L);
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(10);

        String payload = """
                {
                  "id": "evt_pro_1",
                  "object": "event",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_test_1",
                      "object": "checkout.session",
                      "payment_status": "paid",
                      "subscription": "sub_123",
                      "metadata": {"salonId": "1", "tipoCompra": "SUSCRIPCION_PRO"}
                    }
                  }
                }
                """;
        String firma = firmarPayload(payload, Instant.now(clock).getEpochSecond());

        when(eventoStripeRepository.existsById("evt_pro_1")).thenReturn(false);
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));

        WebhookResponse resp = pagoService.procesarWebhook(payload, firma);

        assertTrue(resp.procesado());
        assertEquals(Plan.PRO, salon.getPlan());
        assertEquals(310, salon.getCreditosIa());
        assertEquals("sub_123", salon.getStripeSubscriptionId());
        verify(eventoStripeRepository).save(any());
    }

    @Test
    void procesarWebhook_suscripcionCancelada_vuelveAGratisYConservaCreditos() throws Exception {
        Salon salon = new Salon();
        salon.setId(1L);
        salon.setPlan(Plan.PRO);
        salon.setCreditosIa(250);
        salon.setStripeSubscriptionId("sub_123");

        String payload = """
                {
                  "id": "evt_cancel_1",
                  "object": "event",
                  "type": "customer.subscription.deleted",
                  "data": {
                    "object": {
                      "id": "sub_123",
                      "object": "subscription"
                    }
                  }
                }
                """;
        String firma = firmarPayload(payload, Instant.now(clock).getEpochSecond());

        when(eventoStripeRepository.existsById("evt_cancel_1")).thenReturn(false);
        when(salonRepository.findByStripeSubscriptionId("sub_123")).thenReturn(Optional.of(salon));

        WebhookResponse resp = pagoService.procesarWebhook(payload, firma);

        assertTrue(resp.procesado());
        assertEquals(Plan.GRATIS, salon.getPlan());
        assertEquals(250, salon.getCreditosIa());
        assertNull(salon.getStripeSubscriptionId());
    }
}

