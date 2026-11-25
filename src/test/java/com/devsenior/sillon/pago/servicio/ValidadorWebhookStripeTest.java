
package com.devsenior.sillon.pago.servicio;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devsenior.sillon.compartido.excepcion.ExcepcionSolicitudInvalida;
import com.devsenior.sillon.pago.configuracion.PropiedadesStripe;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.stripe.model.Event;

class ValidadorWebhookStripeTest {

    private ValidadorWebhookStripe validador;

    @BeforeEach
    void preparar() {

        PropiedadesStripe propiedades = new PropiedadesStripe(
                "sk_test_ejemplo",
                "whsec_ejemplo",
                "price_pro_ejemplo",
                "price_creditos_ejemplo",
                "https://api.stripe.com");

        validador = new ValidadorWebhookStripe(propiedades);
    }

    @Test
    void webhookSinFirmaEsRechazado() {

        assertThrows(
                ExcepcionSolicitudInvalida.class,
                () -> validador.validar(
                        "{\"id\":\"evt_123\"}",
                        null));
    }

    @Test
    void webhookConFirmaInvalidaEsRechazado() {

        assertThrows(
                ExcepcionSolicitudInvalida.class,
                () -> validador.validar(
                        "{\"id\":\"evt_123\"}",
                        "firma_invalida"));
    }

    @Test
    void webhookSinCuerpoEsRechazado() {

        assertThrows(
                ExcepcionSolicitudInvalida.class,
                () -> validador.validar(
                        "",
                        "firma_invalida"));
    }

    @Test
    void webhookConFirmaValidaEsAceptado() throws Exception {

        String cuerpo = """
                {
                  "id": "evt_firma_valida",
                  "object": "event",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_test_123",
                      "object": "checkout.session"
                    }
                  }
                }
                """;

        long timestamp = Instant.now().getEpochSecond();

        String contenidoFirmado = timestamp + "." + cuerpo;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                "whsec_ejemplo".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"));

        String firmaCalculada = HexFormat.of().formatHex(
                mac.doFinal(
                        contenidoFirmado.getBytes(StandardCharsets.UTF_8)));

        String cabecera = "t=" + timestamp + ",v1=" + firmaCalculada;

        Event evento = validador.validar(cuerpo, cabecera);

        assertNotNull(evento);
        assertEquals("evt_firma_valida", evento.getId());
        assertEquals("checkout.session.completed", evento.getType());
    }

}
