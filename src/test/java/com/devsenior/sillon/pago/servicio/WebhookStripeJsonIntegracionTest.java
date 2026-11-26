
package com.devsenior.sillon.pago.servicio;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.pago.dto.RespuestaWebhook;
import com.devsenior.sillon.pago.repositorio.EventoStripeRepositorio;
import com.devsenior.sillon.salon.modelo.ConfiguracionSalon;
import com.devsenior.sillon.salon.modelo.Plan;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;
import com.stripe.model.Event;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
                "sillon.stripe.secreto-webhook=whsec_prueba_local"
})
class WebhookStripeJsonIntegracionTest {

        private static final String SECRETO = "whsec_prueba_local";

        @Autowired
        private ValidadorWebhookStripe validador;

        @Autowired
        private ProcesadorWebhookStripe procesador;

        @Autowired
        private SalonRepositorio salonRepositorio;

        @Autowired
        private EventoStripeRepositorio eventoRepositorio;

        @Autowired
        private EntityManager entityManager;

        @Test
        void webhookJsonFirmadoActivaProSinDuplicarCreditos()
                        throws Exception {

                Salon salon = new Salon();
                salon.setNombre("Salon Webhook JSON");
                salon.setSlug("webhook-json-" + UUID.randomUUID());
                salon.setCiudad("Bogota");
                salon.setDireccion("Direccion de prueba 123");
                salon.setTelefono("3001234567");
                salon.setPlan(Plan.GRATIS);
                salon.setCreditosIa(10);
                salon.setConfiguracion(
                                ConfiguracionSalon.predeterminada());
                salon.setFechaCreacion(
                                LocalDateTime.of(2026, 11, 25, 15, 0));

                salon = salonRepositorio.saveAndFlush(salon);

                Long salonId = salon.getId();
                String eventoId = "evt_json_"
                                + UUID.randomUUID().toString().replace("-", "");

                String cuerpo = """
                                {
                                  "id": "%s",
                                  "object": "event",
                                  "api_version": "2026-11-18.endive",
                                  "type": "checkout.session.completed",
                                  "data": {
                                    "object": {
                                      "id": "cs_test_json_123",
                                      "object": "checkout.session",
                                      "mode": "subscription",
                                      "payment_status": "paid",
                                      "client_reference_id": "%d",
                                      "subscription": "sub_json_123",
                                      "metadata": {
                                        "salonId": "%d",
                                        "tipoCompra": "SUSCRIPCION_PRO"
                                      }
                                    }
                                  }
                                }
                                """.formatted(
                                eventoId,
                                salonId,
                                salonId);

                String firma = firmar(cuerpo);

                Event evento = validador.validar(cuerpo, firma);

                assertEquals(eventoId, evento.getId());
                assertEquals(
                                "checkout.session.completed",
                                evento.getType());

                RespuestaWebhook primera = procesador.procesar(evento);

                assertTrue(primera.recibido());
                assertTrue(primera.procesado());

                entityManager.flush();
                entityManager.clear();

                Salon actualizado = salonRepositorio.findById(salonId)
                                .orElseThrow();

                assertEquals(Plan.PRO, actualizado.getPlan());
                assertEquals(310, actualizado.getCreditosIa());
                assertEquals(
                                "sub_json_123",
                                actualizado.getStripeSuscripcionId());

                assertTrue(eventoRepositorio.existsById(eventoId));

                RespuestaWebhook repetido = procesador.procesar(evento);

                assertTrue(repetido.recibido());
                assertFalse(repetido.procesado());

                entityManager.flush();
                entityManager.clear();

                Salon despuesDelDuplicado = salonRepositorio
                                .findById(salonId)
                                .orElseThrow();

                assertEquals(
                                310,
                                despuesDelDuplicado.getCreditosIa());
        }

        private String firmar(String cuerpo) throws Exception {

                long timestamp = Instant.now().getEpochSecond();

                String contenido = timestamp + "." + cuerpo;

                Mac mac = Mac.getInstance("HmacSHA256");

                mac.init(new SecretKeySpec(
                                SECRETO.getBytes(StandardCharsets.UTF_8),
                                "HmacSHA256"));

                byte[] bytes = mac.doFinal(
                                contenido.getBytes(StandardCharsets.UTF_8));

                return "t=" + timestamp
                                + ",v1=" + HexFormat.of().formatHex(bytes);
        }
}
