package com.devsenior.sillon.pago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devsenior.sillon.Datos;
import com.devsenior.sillon.compartido.FirmaInvalidaException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.stripe.StripeClient;

/** Tests del webhook de Stripe con eventos firmados igual que los firma Stripe (HMAC-SHA256). */
class PagoWebhookTest {

    private static final String SECRETO = "whsec_prueba";

    private final SalonRepository salonRepository = mock(SalonRepository.class);
    private final EventoStripeRepository eventoRepository = mock(EventoStripeRepository.class);
    private PagoService pagoService;
    private Salon salon;

    @BeforeEach
    void preparar() {
        pagoService = new PagoService(mock(StripeClient.class), salonRepository, eventoRepository, Datos.relojFijo(),
                "price_pro", "price_creditos", SECRETO, "http://localhost:8080", 300, 200);
        salon = Datos.salon(Plan.GRATIS);
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
    }

    private static String eventoCompra(String eventoId, String tipo, String suscripcion) {
        String sub = suscripcion == null ? "null" : "\"" + suscripcion + "\"";
        return """
                {"id":"%s","object":"event","type":"checkout.session.completed","data":{"object":{"id":"cs_test_1",\
                "object":"checkout.session","payment_status":"paid","subscription":%s,\
                "metadata":{"salonId":"1","tipo":"%s"}}}}""".formatted(eventoId, sub, tipo);
    }

    private static String firmar(String payload, String secreto) throws Exception {
        long t = System.currentTimeMillis() / 1000;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String firma = HexFormat.of().formatHex(mac.doFinal((t + "." + payload).getBytes(StandardCharsets.UTF_8)));
        return "t=" + t + ",v1=" + firma;
    }

    @Test
    void pagoProActivaElPlanYSuma300Creditos() throws Exception {
        String payload = eventoCompra("evt_pro", "SUSCRIPCION_PRO", "sub_123");

        WebhookRespuesta r = pagoService.procesarWebhook(payload, firmar(payload, SECRETO));

        assertThat(r.procesado()).isTrue();
        assertThat(salon.getPlan()).isEqualTo(Plan.PRO);
        assertThat(salon.getCreditosIa()).isEqualTo(310);
        assertThat(salon.getStripeSuscripcionId()).isEqualTo("sub_123");
        verify(eventoRepository).save(any(EventoStripe.class));
    }

    @Test
    void paqueteDeCreditosSoloSuma200() throws Exception {
        String payload = eventoCompra("evt_paquete", "PAQUETE_CREDITOS", null);

        pagoService.procesarWebhook(payload, firmar(payload, SECRETO));

        assertThat(salon.getPlan()).isEqualTo(Plan.GRATIS);
        assertThat(salon.getCreditosIa()).isEqualTo(210);
    }

    @Test
    void eventoRepetidoNoSeAplicaDosVeces() throws Exception {
        when(eventoRepository.existsById("evt_pro")).thenReturn(true);
        String payload = eventoCompra("evt_pro", "SUSCRIPCION_PRO", "sub_123");

        WebhookRespuesta r = pagoService.procesarWebhook(payload, firmar(payload, SECRETO));

        assertThat(r.procesado()).isFalse();
        assertThat(salon.getCreditosIa()).isEqualTo(10);
        verify(salonRepository, never()).save(any());
    }

    @Test
    void cancelacionDeLaSuscripcionVuelveAGratisYConservaCreditos() throws Exception {
        salon.setPlan(Plan.PRO);
        salon.setCreditosIa(310);
        salon.setStripeSuscripcionId("sub_123");
        when(salonRepository.findByStripeSuscripcionId("sub_123")).thenReturn(Optional.of(salon));
        String payload = """
                {"id":"evt_cancela","object":"event","type":"customer.subscription.deleted",\
                "data":{"object":{"id":"sub_123","object":"subscription","status":"canceled"}}}""";

        WebhookRespuesta r = pagoService.procesarWebhook(payload, firmar(payload, SECRETO));

        assertThat(r.procesado()).isTrue();
        assertThat(salon.getPlan()).isEqualTo(Plan.GRATIS);
        assertThat(salon.getCreditosIa()).isEqualTo(310);
    }

    @Test
    void webhookConFirmaFalsaDa400() throws Exception {
        String payload = eventoCompra("evt_falso", "SUSCRIPCION_PRO", "sub_x");

        assertThatThrownBy(() -> pagoService.procesarWebhook(payload, firmar(payload, "whsec_otro")))
                .isInstanceOf(FirmaInvalidaException.class);
        verify(salonRepository, never()).save(any());
    }

    @Test
    void webhookSinFirmaDa400() {
        assertThatThrownBy(() -> pagoService.procesarWebhook("{\"id\":\"evt\"}", null))
                .isInstanceOf(FirmaInvalidaException.class);
    }

    @Test
    void salonYaProNoPuedePedirOtraSuscripcion() {
        salon.setPlan(Plan.PRO);
        assertThatThrownBy(() -> pagoService.crearSesion(1L, "camilo@navaja.co", TipoCompra.SUSCRIPCION_PRO))
                .isInstanceOf(ReglaNegocioException.class);
    }
}
