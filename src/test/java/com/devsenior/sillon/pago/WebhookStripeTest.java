package com.devsenior.sillon.pago;

import com.devsenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.pago.dto.ResultadoPagoRespuesta;
import com.devsenior.sillon.pago.dto.SesionPagoRespuesta;
import com.devsenior.sillon.pago.dto.WebhookRespuesta;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.usuario.Rol;
import com.stripe.StripeClient;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.service.CheckoutService;
import com.stripe.service.checkout.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookStripeTest {

    private static final String SECRETO_WEBHOOK = "whsec_test_secret_sillon_2026";

    @Mock
    private StripeClient stripeClient;
    @Mock
    private SalonRepository salonRepository;
    @Mock
    private EventoStripeRepository eventoStripeRepository;

    private PagoService pagoService;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.now(), ZoneId.of("America/Bogota"));
        pagoService = new PagoService(
                stripeClient,
                salonRepository,
                eventoStripeRepository,
                new ObjectMapper(),
                clock,
                "price_pro_test",
                "price_creditos_test",
                SECRETO_WEBHOOK,
                "http://localhost:8080",
                300,
                200
        );
    }

    @Test
    void webhookSinFirmaLanza400() {
        assertThrows(FirmaInvalidaException.class, () -> pagoService.procesarWebhook("{}", null));
        assertThrows(FirmaInvalidaException.class, () -> pagoService.procesarWebhook("{}", "  "));
    }

    @Test
    void webhookConFirmaInvalidaLanza400() {
        String payload = "{\"id\":\"evt_1\",\"type\":\"checkout.session.completed\"}";
        assertThrows(FirmaInvalidaException.class, () -> pagoService.procesarWebhook(payload, "t=12345,v1=firmamala"));
    }

    @Test
    void webhookCheckoutProCambiaPlanAProYSuma300Creditos() throws Exception {
        Salon salon = crearSalonGratis(1L, 10);
        when(eventoStripeRepository.existsById("evt_pro_1")).thenReturn(false);
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));

        String payload = """
                {
                  "id": "evt_pro_1",
                  "object": "event",
                  "type": "checkout.session.completed",
                  "api_version": "2024-06-20",
                  "data": {
                    "object": {
                      "id": "cs_test_1",
                      "object": "checkout.session",
                      "mode": "subscription",
                      "payment_status": "paid",
                      "subscription": "sub_12345",
                      "metadata": {
                        "salonId": "1",
                        "tipoCompra": "SUSCRIPCION_PRO"
                      }
                    }
                  }
                }
                """;
        String sig = firmarPayload(payload, SECRETO_WEBHOOK);

        WebhookRespuesta respuesta = pagoService.procesarWebhook(payload, sig);

        assertTrue(respuesta.recibido());
        assertTrue(respuesta.procesado());
        assertEquals(Plan.PRO, salon.getPlan());
        assertEquals("sub_12345", salon.getStripeSuscripcionId());
        assertEquals(310, salon.getCreditosIa());
        verify(salonRepository).saveAndFlush(salon);
        verify(eventoStripeRepository).save(any(EventoStripe.class));
    }

    @Test
    void webhookCheckoutPaqueteCreditosSuma200Creditos() throws Exception {
        Salon salon = crearSalonGratis(1L, 310);
        salon.setPlan(Plan.PRO);
        when(eventoStripeRepository.existsById("evt_cred_1")).thenReturn(false);
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));

        String payload = """
                {
                  "id": "evt_cred_1",
                  "object": "event",
                  "type": "checkout.session.completed",
                  "api_version": "2024-06-20",
                  "data": {
                    "object": {
                      "id": "cs_test_2",
                      "object": "checkout.session",
                      "mode": "payment",
                      "payment_status": "paid",
                      "metadata": {
                        "salonId": "1",
                        "tipoCompra": "PAQUETE_CREDITOS"
                      }
                    }
                  }
                }
                """;
        String sig = firmarPayload(payload, SECRETO_WEBHOOK);

        WebhookRespuesta respuesta = pagoService.procesarWebhook(payload, sig);

        assertTrue(respuesta.procesado());
        assertEquals(510, salon.getCreditosIa());
        verify(salonRepository).saveAndFlush(salon);
    }

    @Test
    void webhookRepetidoEsIdempotenteYNoSumaCreditos() throws Exception {
        when(eventoStripeRepository.existsById("evt_rep_1")).thenReturn(true);

        String payload = """
                {
                  "id": "evt_rep_1",
                  "object": "event",
                  "type": "checkout.session.completed",
                  "data": { "object": {} }
                }
                """;
        String sig = firmarPayload(payload, SECRETO_WEBHOOK);

        WebhookRespuesta respuesta = pagoService.procesarWebhook(payload, sig);

        assertTrue(respuesta.recibido());
        assertFalse(respuesta.procesado());
        verify(salonRepository, never()).saveAndFlush(any());
        verify(eventoStripeRepository, never()).save(any());
    }

    @Test
    void webhookSubscriptionDeletedVuelveAGratisYConservaCreditos() throws Exception {
        Salon salon = crearSalonGratis(1L, 510);
        salon.setPlan(Plan.PRO);
        salon.setStripeSuscripcionId("sub_cancel_1");

        when(eventoStripeRepository.existsById("evt_del_1")).thenReturn(false);
        when(salonRepository.findByStripeSuscripcionId("sub_cancel_1")).thenReturn(Optional.of(salon));

        String payload = """
                {
                  "id": "evt_del_1",
                  "object": "event",
                  "type": "customer.subscription.deleted",
                  "data": {
                    "object": {
                      "id": "sub_cancel_1",
                      "object": "subscription"
                    }
                  }
                }
                """;
        String sig = firmarPayload(payload, SECRETO_WEBHOOK);

        WebhookRespuesta respuesta = pagoService.procesarWebhook(payload, sig);

        assertTrue(respuesta.procesado());
        assertEquals(Plan.GRATIS, salon.getPlan());
        assertNull(salon.getStripeSuscripcionId());
        assertEquals(510, salon.getCreditosIa());
    }

    @Test
    void crearSuscripcionSiYaEsProLanza409() {
        Salon salon = crearSalonGratis(1L, 310);
        salon.setPlan(Plan.PRO);
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));

        UsuarioPrincipal dueno = new UsuarioPrincipal(5L, 1L, "camilo@salon.com", "hash", Rol.DUENO, true);
        assertThrows(ReglaNegocioException.class, () -> pagoService.crearSesionSuscripcion(dueno));
    }

    @Test
    void crearSuscripcionEnPlanGratisInvocaStripeClient() throws Exception {
        Salon salon = crearSalonGratis(1L, 10);
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));

        CheckoutService checkoutService = mock(CheckoutService.class);
        SessionService sessionService = mock(SessionService.class);
        Session sessionMock = new Session();
        sessionMock.setId("cs_nueva_1");
        sessionMock.setUrl("https://checkout.stripe.com/pay/cs_nueva_1");

        when(stripeClient.checkout()).thenReturn(checkoutService);
        when(checkoutService.sessions()).thenReturn(sessionService);
        when(sessionService.create(any(SessionCreateParams.class))).thenReturn(sessionMock);

        UsuarioPrincipal dueno = new UsuarioPrincipal(5L, 1L, "camilo@salon.com", "hash", Rol.DUENO, true);
        SesionPagoRespuesta resp = pagoService.crearSesionSuscripcion(dueno);

        assertEquals(TipoCompra.SUSCRIPCION_PRO, resp.tipo());
        assertEquals("cs_nueva_1", resp.sesionId());
        assertEquals("https://checkout.stripe.com/pay/cs_nueva_1", resp.urlPago());
    }

    @Test
    void obtenerResultadoExitoYCancelado() {
        ResultadoPagoRespuesta exito = pagoService.obtenerResultado("exito");
        ResultadoPagoRespuesta cancelado = pagoService.obtenerResultado("cancelado");

        assertEquals("exito", exito.estado());
        assertTrue(exito.mensaje().toLowerCase().contains("exitosamente"));
        assertEquals("cancelado", cancelado.estado());
        assertTrue(cancelado.mensaje().toLowerCase().contains("cancelado"));
    }

    @Test
    void webhookCheckoutUnpaidNoAcreditaPlanNiCreditos() throws Exception {
        when(eventoStripeRepository.existsById("evt_unpaid_1")).thenReturn(false);

        String payload = """
                {
                  "id": "evt_unpaid_1",
                  "object": "event",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_unpaid",
                      "payment_status": "unpaid",
                      "metadata": { "salonId": "1", "tipoCompra": "SUSCRIPCION_PRO" }
                    }
                  }
                }
                """;
        String sig = firmarPayload(payload, SECRETO_WEBHOOK);

        WebhookRespuesta respuesta = pagoService.procesarWebhook(payload, sig);

        assertTrue(respuesta.procesado());
        verify(salonRepository, never()).saveAndFlush(any());
    }

    private Salon crearSalonGratis(Long id, int creditos) {
        Salon salon = new Salon();
        salon.setId(id);
        salon.setNombre("Barberia Test");
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(creditos);
        return salon;
    }

    private String firmarPayload(String payload, String secreto) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String firmado = timestamp + "." + payload;
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256Hmac.init(secretKey);
        byte[] hash = sha256Hmac.doFinal(firmado.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return "t=" + timestamp + ",v1=" + hex;
    }
}
