package com.devsenior.sillon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import tools.jackson.databind.*;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:flujos;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "sillon.stripe.webhook=firma-de-pruebas"
        })
@AutoConfigureMockMvc
class FlujosDelSalonTest {
    @Autowired MockMvc api;
    @Autowired ObjectMapper json;
    @Autowired Clock reloj;
    String token, slug, email;
    long salon, servicio, mateo;
    LocalDate dia;

    JsonNode pedir(HttpMethod metodo, String ruta, Object cuerpo, int esperado) throws Exception {
        var req = MockMvcRequestBuilders.request(metodo, ruta);
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (cuerpo != null)
            req.contentType("application/json").content(json.writeValueAsBytes(cuerpo));
        var bytes =
                api.perform(req)
                        .andExpect(status().is(esperado))
                        .andReturn()
                        .getResponse()
                        .getContentAsByteArray();
        return json.readTree(bytes);
    }

    JsonNode nuevoEstilista(String nombre, int esperado) throws Exception {
        return pedir(
                HttpMethod.POST,
                "/api/estilistas",
                Map.of(
                        "nombre",
                        nombre,
                        "email",
                        nombre + email,
                        "password",
                        "Prueba-123",
                        "servicioIds",
                        List.of(servicio)),
                esperado);
    }

    @BeforeEach
    void preparar() throws Exception {
        email = UUID.randomUUID() + "@example.test";
        var r =
                pedir(
                        HttpMethod.POST,
                        "/api/auth/registro",
                        Map.of(
                                "nombre",
                                "Eva",
                                "salonNombre",
                                "Sol",
                                "email",
                                email,
                                "password",
                                "Prueba-123",
                                "ciudad",
                                "Cali",
                                "direccion",
                                "Calle 8",
                                "telefono",
                                "3109876543"),
                        201);
        token = r.get("token").asText();
        slug = r.get("salonSlug").asText();
        salon = r.get("salonId").asLong();
        servicio =
                pedir(
                                HttpMethod.POST,
                                "/api/servicios",
                                Map.of("nombre", "Corte", "duracionMinutos", 45, "precio", 35000),
                                201)
                        .get("id")
                        .asLong();
        mateo = nuevoEstilista("Mateo", 201).get("id").asLong();
        dia = LocalDate.now(reloj).plusDays(3);
        if (dia.getDayOfWeek() == DayOfWeek.SUNDAY) dia = dia.plusDays(1);
    }

    JsonNode cita(LocalDateTime inicio, int esperado) throws Exception {
        return pedir(
                HttpMethod.POST,
                "/api/citas",
                Map.of(
                        "clienteNombre",
                        "Ana",
                        "clienteTelefono",
                        "+57 3101234567",
                        "inicio",
                        inicio.toString(),
                        "estilistaId",
                        mateo,
                        "servicioId",
                        servicio),
                esperado);
    }

    @Test
    void precioHistoricoYFranjasConsecutivas() throws Exception {
        var inicial = cita(dia.atTime(10, 0), 201);
        assertThat(inicial.get("estado").asText()).isEqualTo("CONFIRMADA");
        assertThat(inicial.get("clienteTelefono").asText()).isEqualTo("+573101234567");
        pedir(
                HttpMethod.PUT,
                "/api/servicios/" + servicio,
                Map.of("nombre", "Corte", "duracionMinutos", 30, "precio", 40000),
                200);
        assertThat(cita(dia.atTime(10, 45), 201).get("precio").asInt()).isEqualTo(40000);
        var agenda = pedir(HttpMethod.GET, "/api/citas?fecha=" + dia, null, 200);
        assertThat(agenda.get(0).get("precio").asInt()).isEqualTo(35000);
        assertThat(pedir(HttpMethod.GET, "/api/clientes", null, 200).size()).isEqualTo(1);
    }

    @Test
    void cruceYPasadoDejanIntactaLaAgenda() throws Exception {
        cita(dia.atTime(10, 0), 201);
        cita(dia.atTime(10, 30), 409);
        cita(dia.minusWeeks(1).atTime(10, 0), 409);
        assertThat(pedir(HttpMethod.GET, "/api/citas?fecha=" + dia, null, 200).size()).isEqualTo(1);
    }

    @Test
    void servicioDesactivadoSaleDelCatalogo() throws Exception {
        pedir(
                HttpMethod.PUT,
                "/api/servicios/" + servicio,
                Map.of("nombre", "Corte", "duracionMinutos", 45, "precio", 35000, "activo", false),
                200);
        var catalogo = pedir(HttpMethod.GET, "/api/publico/salones/" + slug, null, 200);
        assertThat(catalogo.get("servicios").size()).isZero();
        cita(dia.atTime(11, 0), 409);
    }

    @Test
    void dosReservasConcurrentesObtienenUnSoloTurno() throws Exception {
        var inicio = dia.atTime(11, 0).toString();
        var body =
                json.writeValueAsBytes(
                        Map.of(
                                "clienteNombre",
                                "Ana",
                                "clienteTelefono",
                                "3109876543",
                                "inicio",
                                inicio,
                                "servicioId",
                                servicio,
                                "estilistaId",
                                mateo));
        var barrera = new java.util.concurrent.CyclicBarrier(2);
        java.util.concurrent.Callable<Integer> reservar =
                () -> {
                    barrera.await(5, java.util.concurrent.TimeUnit.SECONDS);
                    return api.perform(
                                    MockMvcRequestBuilders.post(
                                                    "/api/publico/salones/" + slug + "/reservas")
                                            .contentType("application/json")
                                            .content(body))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                };
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(reservar);
            var b = pool.submit(reservar);
            assertThat(
                            List.of(
                                    a.get(15, java.util.concurrent.TimeUnit.SECONDS),
                                    b.get(15, java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        } finally {
            pool.shutdownNow();
        }
    }

    JsonNode webhook(String id, String tipo, Map<String, Object> objeto) throws Exception {
        var body =
                json.writeValueAsString(
                        Map.of("id", id, "type", tipo, "data", Map.of("object", objeto)));
        long t = reloj.instant().getEpochSecond();
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(
                new SecretKeySpec(
                        "firma-de-pruebas".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        var firma =
                HexFormat.of()
                        .formatHex(mac.doFinal((t + "." + body).getBytes(StandardCharsets.UTF_8)));
        var r =
                api.perform(
                                MockMvcRequestBuilders.post("/api/pagos/webhook")
                                        .header("Stripe-Signature", "t=" + t + ",v1=" + firma)
                                        .contentType("application/json")
                                        .content(body))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse();
        return json.readTree(r.getContentAsByteArray());
    }

    @Test
    void cicloStripeYLimiteDeEquipo() throws Exception {
        nuevoEstilista("Sara", 201);
        nuevoEstilista("Luis", 402);
        String sub = "sub_" + salon;
        String evt = "evt_" + salon;
        var meta = Map.of("salonId", String.valueOf(salon), "tipo", "SUSCRIPCION_PRO");
        Map<String, Object> checkout =
                Map.of(
                        "id",
                        "cs_" + salon,
                        "mode",
                        "subscription",
                        "subscription",
                        sub,
                        "payment_status",
                        "paid",
                        "metadata",
                        meta);
        assertThat(
                        webhook(evt, "checkout.session.completed", checkout)
                                .get("procesado")
                                .asBoolean())
                .isTrue();
        assertThat(
                        webhook(evt, "checkout.session.completed", checkout)
                                .get("procesado")
                                .asBoolean())
                .isFalse();
        nuevoEstilista("Luis", 201);
        var pro = pedir(HttpMethod.GET, "/api/salon", null, 200);
        assertThat(pro.get("plan").asText()).isEqualTo("PRO");
        assertThat(pro.get("creditosIa").asInt()).isEqualTo(310);
        webhook(
                evt + "p",
                "checkout.session.completed",
                Map.of(
                        "id",
                        "paquete_" + salon,
                        "mode",
                        "payment",
                        "payment_status",
                        "paid",
                        "metadata",
                        Map.of("salonId", salon, "tipo", "PAQUETE_CREDITOS")));
        webhook(evt + "c", "customer.subscription.deleted", Map.of("id", sub));
        var gratis = pedir(HttpMethod.GET, "/api/salon", null, 200);
        assertThat(gratis.get("plan").asText()).isEqualTo("GRATIS");
        assertThat(gratis.get("creditosIa").asInt()).isEqualTo(510);
    }
}
