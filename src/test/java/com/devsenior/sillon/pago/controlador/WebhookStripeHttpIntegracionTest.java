
package com.devsenior.sillon.pago.controlador;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.pago.repositorio.EventoStripeRepositorio;
import com.devsenior.sillon.salon.modelo.ConfiguracionSalon;
import com.devsenior.sillon.salon.modelo.Plan;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "sillon.stripe.secreto-webhook=whsec_prueba_http"
})
class WebhookStripeHttpIntegracionTest {

    private static final String SECRETO = "whsec_prueba_http";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SalonRepositorio salonRepositorio;

    @Autowired
    private EventoStripeRepositorio eventoRepositorio;

    @Autowired
    private EntityManager entityManager;

    @Test
    void webhookFirmadoActivaProPorHttpSinDuplicarCreditos()
            throws Exception {

        Salon salon = new Salon();
        salon.setNombre("Salon Prueba HTTP Stripe");
        salon.setSlug("stripe-http-" + UUID.randomUUID());
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

        String eventoId = "evt_http_"
                + UUID.randomUUID().toString().replace("-", "");

        String suscripcionId = "sub_http_"
                + UUID.randomUUID().toString().replace("-", "");

        String cuerpo = """
                {
                  "id": "%s",
                  "object": "event",
                  "api_version": "2026-09-30.endive",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_test_http_123",
                      "object": "checkout.session",
                      "mode": "subscription",
                      "payment_status": "paid",
                      "client_reference_id": "%d",
                      "subscription": "%s",
                      "metadata": {
                        "salonId": "%d",
                        "tipo": "SUSCRIPCION_PRO"
                      }
                    }
                  }
                }
                """.formatted(
                eventoId,
                salonId,
                suscripcionId,
                salonId);

        String firma = firmar(cuerpo);

        mvc.perform(post("/api/pagos/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", firma)
                .content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recibido").value(true))
                .andExpect(jsonPath("$.procesado").value(true));

        entityManager.flush();
        entityManager.clear();

        Salon actualizado = salonRepositorio.findById(salonId)
                .orElseThrow();

        assertEquals(Plan.PRO, actualizado.getPlan());
        assertEquals(310, actualizado.getCreditosIa());
        assertEquals(
                suscripcionId,
                actualizado.getStripeSuscripcionId());

        assertTrue(eventoRepositorio.existsById(eventoId));

        mvc.perform(post("/api/pagos/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", firma)
                .content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recibido").value(true))
                .andExpect(jsonPath("$.procesado").value(false));

        entityManager.flush();
        entityManager.clear();

        Salon despuesDelDuplicado = salonRepositorio
                .findById(salonId)
                .orElseThrow();

        assertEquals(Plan.PRO, despuesDelDuplicado.getPlan());
        assertEquals(
                310,
                despuesDelDuplicado.getCreditosIa());

        assertEquals(
                suscripcionId,
                despuesDelDuplicado.getStripeSuscripcionId());
    }

    private String firmar(String cuerpo) throws Exception {

        long timestamp = Instant.now().getEpochSecond();

        String contenido = timestamp + "." + cuerpo;

        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(new SecretKeySpec(
                SECRETO.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"));

        byte[] firma = mac.doFinal(
                contenido.getBytes(StandardCharsets.UTF_8));

        return "t=" + timestamp
                + ",v1=" + HexFormat.of().formatHex(firma);
    }
}
