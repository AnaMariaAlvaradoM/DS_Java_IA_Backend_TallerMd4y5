package com.devsenior.sillon.pago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.stripe.Stripe;

import jakarta.persistence.EntityManager;

@SpringBootTest(properties = {
        "sillon.stripe.clave-secreta=sk_test_simulada",
        "sillon.stripe.secreto-webhook=whsec_simulado_solo_para_tests",
        "sillon.creditos.pro=300",
        "sillon.creditos.paquete=200"
})
@AutoConfigureMockMvc
class WebhookStripeIntegracionTest {

    private static final String SECRETO = "whsec_simulado_solo_para_tests";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private Clock clock;

    private final List<Long> salones = new ArrayList<>();
    private final List<String> eventos = new ArrayList<>();
    private Long salonId;

    @BeforeEach
    void preparar() {
        salonId = crearSalon();
    }

    @AfterEach
    void limpiar() {
        for (String id : eventos) {
            jdbc.update("delete from eventos_stripe where id = ?", id);
        }
        for (Long id : salones) {
            jdbc.update("delete from salones where id = ?", id);
        }
    }

    @Test
    void pagoProFirmadoSinJwtActivaBeneficiosYRegistraElEvento() throws Exception {
        Long ajeno = crearSalon();
        String id = nuevoEvento();
        enviar(checkout(id, salonId, "SUSCRIPCION_PRO", "paid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.recibido").value(true))
                .andExpect(jsonPath("$.procesado").value(true));

        assertThat(plan(salonId)).isEqualTo("PRO");
        assertThat(creditos(salonId)).isEqualTo(310);
        assertThat(jdbc.queryForObject("select stripe_suscripcion_id from salones where id = ?", String.class, salonId))
                .isEqualTo("sub_simulada");
        assertThat(plan(ajeno)).isEqualTo("GRATIS");
        assertThat(creditos(ajeno)).isEqualTo(10);
        assertThat(contarEvento(id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select tipo from eventos_stripe where id = ?", String.class, id))
                .isEqualTo("checkout.session.completed");
        assertThat(jdbc.queryForObject("select fecha_procesado from eventos_stripe where id = ?",
                LocalDateTime.class, id)).isNotNull();
    }

    @Test
    void paqueteSumaLaCantidadDelServidorSinCambiarPlanOSuscripcion() throws Exception {
        jdbc.update("update salones set plan = 'PRO', stripe_suscripcion_id = 'sub_previa' where id = ?", salonId);
        String cuerpo = checkout(nuevoEvento(), salonId, "PAQUETE_CREDITOS", "paid")
                .replace("\"tipo\":\"PAQUETE_CREDITOS\"", "\"tipo\":\"PAQUETE_CREDITOS\",\"creditos\":\"99999\"");
        enviar(cuerpo).andExpect(status().isOk()).andExpect(jsonPath("$.procesado").value(true));
        assertThat(creditos(salonId)).isEqualTo(210);
        assertThat(plan(salonId)).isEqualTo("PRO");
        assertThat(jdbc.queryForObject("select stripe_suscripcion_id from salones where id = ?", String.class, salonId))
                .isEqualTo("sub_previa");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ausente", "invalida", "otro-secreto", "caducada"})
    void rechazaFirmasAusentesFalsasOCaducadasSinGuardarNada(String caso) throws Exception {
        String id = nuevoEvento();
        String cuerpo = checkout(id, salonId, "SUSCRIPCION_PRO", "paid");
        var solicitud = post("/api/pagos/webhook").contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        if (!caso.equals("ausente")) {
            String firma = switch (caso) {
                case "otro-secreto" -> firmar(cuerpo, "whsec_otro_simulado", clock.instant().getEpochSecond());
                case "caducada" -> firmar(cuerpo, SECRETO, clock.instant().getEpochSecond() - 600);
                default -> "t=1,v1=firma-falsa";
            };
            solicitud.header("Stripe-Signature", firma);
        }
        mockMvc.perform(solicitud).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensaje").isString())
                .andExpect(jsonPath("$.fecha").isString());
        assertThat(contarEvento(id)).isZero();
        assertThat(creditos(salonId)).isEqualTo(10);
        assertThat(plan(salonId)).isEqualTo("GRATIS");
    }

    @Test
    void alterarElCuerpoInvalidaLaFirmaAunqueElJsonSigaSiendoValido() throws Exception {
        String id = nuevoEvento();
        String cuerpo = checkout(id, salonId, "PAQUETE_CREDITOS", "paid");
        mockMvc.perform(post("/api/pagos/webhook").contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", firmar(cuerpo, SECRETO, clock.instant().getEpochSecond()))
                .content(cuerpo.replace("PAQUETE_CREDITOS", "SUSCRIPCION_PRO")))
                .andExpect(status().isBadRequest());
        assertThat(contarEvento(id)).isZero();
        assertThat(creditos(salonId)).isEqualTo(10);
    }

    @Test
    void eventoRepetidoNoAplicaBeneficiosDosVeces() throws Exception {
        String id = nuevoEvento();
        String cuerpo = checkout(id, salonId, "SUSCRIPCION_PRO", "paid");
        enviar(cuerpo).andExpect(status().isOk()).andExpect(jsonPath("$.procesado").value(true));
        enviar(cuerpo).andExpect(status().isOk())
                .andExpect(jsonPath("$.recibido").value(true))
                .andExpect(jsonPath("$.procesado").value(false));
        assertThat(creditos(salonId)).isEqualTo(310);
        assertThat(contarEvento(id)).isEqualTo(1);
    }

    @Test
    void otroTipoDeEventoSeRegistraPeroNoAlteraBeneficios() throws Exception {
        String id = nuevoEvento();
        String cuerpo = evento(id, "invoice.paid", "{\"object\":\"invoice\",\"id\":\"in_simulada\"}", Stripe.API_VERSION);
        enviar(cuerpo).andExpect(status().isOk()).andExpect(jsonPath("$.procesado").value(false));
        enviar(cuerpo).andExpect(status().isOk()).andExpect(jsonPath("$.procesado").value(false));
        assertThat(contarEvento(id)).isEqualTo(1);
        assertThat(plan(salonId)).isEqualTo("GRATIS");
        assertThat(creditos(salonId)).isEqualTo(10);
    }

    @ParameterizedTest
    @ValueSource(strings = {"unpaid", "no_payment_required"})
    void checkoutNoPagadoNoOtorgaBeneficios(String estado) throws Exception {
        String id = nuevoEvento();
        enviar(checkout(id, salonId, "SUSCRIPCION_PRO", estado))
                .andExpect(status().isOk()).andExpect(jsonPath("$.procesado").value(false));
        assertThat(contarEvento(id)).isEqualTo(1);
        assertThat(plan(salonId)).isEqualTo("GRATIS");
        assertThat(creditos(salonId)).isEqualTo(10);
    }

    @Test
    void cancelacionBuscaPorSuscripcionYConservaCreditos() throws Exception {
        Long otro = crearSalon();
        jdbc.update("update salones set plan = 'PRO', creditos_ia = 450, stripe_suscripcion_id = ? where id = ?",
                "sub_cancelada", salonId);
        String id = nuevoEvento();
        String cuerpo = evento(id, "customer.subscription.deleted", """
                {"object":"subscription","id":"sub_cancelada","metadata":{"salonId":"%s"}}
                """.formatted(otro), Stripe.API_VERSION);
        enviar(cuerpo).andExpect(status().isOk()).andExpect(jsonPath("$.procesado").value(true));
        enviar(cuerpo).andExpect(status().isOk()).andExpect(jsonPath("$.procesado").value(false));
        assertThat(plan(salonId)).isEqualTo("GRATIS");
        assertThat(creditos(salonId)).isEqualTo(450);
        assertThat(creditos(otro)).isEqualTo(10);
        assertThat(contarEvento(id)).isEqualTo(1);
    }

    @Test
    void salonInexistenteRevierteElEventoYPermiteReintentarlo() throws Exception {
        Long eliminado = crearSalon();
        jdbc.update("delete from salones where id = ?", eliminado);
        String id = nuevoEvento();
        enviar(checkout(id, eliminado, "PAQUETE_CREDITOS", "paid"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value(404));
        assertThat(contarEvento(id)).isZero();
        assertThat(creditos(salonId)).isEqualTo(10);
        enviar(checkout(id, salonId, "PAQUETE_CREDITOS", "paid"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.procesado").value(true));
        assertThat(contarEvento(id)).isEqualTo(1);
        assertThat(creditos(salonId)).isEqualTo(210);
    }

    @Test
    void suscripcionDesconocidaNoMarcaElEventoComoProcesado() throws Exception {
        String id = nuevoEvento();
        enviar(evento(id, "customer.subscription.deleted",
                "{\"object\":\"subscription\",\"id\":\"sub_inexistente\"}", Stripe.API_VERSION))
                .andExpect(status().isNotFound());
        assertThat(contarEvento(id)).isZero();
        assertThat(creditos(salonId)).isEqualTo(10);
    }

    @Test
    void versionDistintaDelSdkUsaLaDeserializacionDelObjetoYaFirmado() throws Exception {
        String id = nuevoEvento();
        String cuerpo = checkout(id, salonId, "PAQUETE_CREDITOS", "paid")
                .replace(Stripe.API_VERSION, "2020-08-27");
        enviar(cuerpo).andExpect(status().isOk()).andExpect(jsonPath("$.procesado").value(true));
        assertThat(creditos(salonId)).isEqualTo(210);
    }

    @ParameterizedTest
    @ValueSource(strings = {"sin-metadata", "salon-invalido", "tipo-invalido", "sin-suscripcion", "objeto-incorrecto"})
    void datosVerificadosIncompletosNoSeConfirmanNiAplicanBeneficios(String caso) throws Exception {
        String id = nuevoEvento();
        String cuerpo = checkout(id, salonId, "SUSCRIPCION_PRO", "paid");
        cuerpo = switch (caso) {
            case "sin-metadata" -> cuerpo.replace("\"metadata\":", "\"otraPropiedad\":");
            case "salon-invalido" -> cuerpo.replace("\"salonId\":\"" + salonId + "\"", "\"salonId\":\"invalido\"");
            case "tipo-invalido" -> cuerpo.replace("SUSCRIPCION_PRO", "DESCONOCIDO");
            case "sin-suscripcion" -> cuerpo.replace("\"subscription\":\"sub_simulada\",", "");
            default -> cuerpo.replace("\"object\":\"checkout.session\"", "\"object\":\"invoice\"");
        };
        enviar(cuerpo).andExpect(status().isBadGateway());
        assertThat(contarEvento(id)).isZero();
        assertThat(creditos(salonId)).isEqualTo(10);
        assertThat(plan(salonId)).isEqualTo("GRATIS");
    }

    @Test
    void dosEntregasConcurrentesDelMismoEventoSoloAplicanUnPaqueteEnMySql() throws Exception {
        String id = nuevoEvento();
        String cuerpo = checkout(id, salonId, "PAQUETE_CREDITOS", "paid");
        assertThat(enviarSimultaneamente(cuerpo, cuerpo)).containsExactlyInAnyOrder(true, false);
        assertThat(creditos(salonId)).isEqualTo(210);
        assertThat(contarEvento(id)).isEqualTo(1);
    }

    @Test
    void pagosDistintosConcurrentesNoPierdenIncrementosDelMismoSalon() throws Exception {
        String primero = nuevoEvento();
        String segundo = nuevoEvento();
        assertThat(enviarSimultaneamente(checkout(primero, salonId, "PAQUETE_CREDITOS", "paid"),
                checkout(segundo, salonId, "PAQUETE_CREDITOS", "paid"))).containsExactly(true, true);
        assertThat(creditos(salonId)).isEqualTo(410);
        assertThat(contarEvento(primero)).isEqualTo(1);
        assertThat(contarEvento(segundo)).isEqualTo(1);
    }

    private List<Boolean> enviarSimultaneamente(String primero, String segundo) throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var barrera = new CyclicBarrier(2);
        try {
            var a = executor.submit(() -> {
                barrera.await(10, TimeUnit.SECONDS);
                return enviar(primero).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            });
            var b = executor.submit(() -> {
                barrera.await(10, TimeUnit.SECONDS);
                return enviar(segundo).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            });
            var mapper = new tools.jackson.databind.ObjectMapper();
            return List.of(mapper.readTree(a.get(20, TimeUnit.SECONDS)).get("procesado").asBoolean(),
                    mapper.readTree(b.get(20, TimeUnit.SECONDS)).get("procesado").asBoolean());
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(25, TimeUnit.SECONDS)).isTrue();
        }
    }

    private ResultActions enviar(String cuerpo) throws Exception {
        return mockMvc.perform(post("/api/pagos/webhook").contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", firmar(cuerpo, SECRETO, clock.instant().getEpochSecond()))
                .content(cuerpo));
    }

    private static String firmar(String cuerpo, String secreto, long timestamp) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String firma = HexFormat.of().formatHex(mac.doFinal((timestamp + "." + cuerpo).getBytes(StandardCharsets.UTF_8)));
        return "t=" + timestamp + ",v1=" + firma;
    }

    private String checkout(String id, Long salon, String tipo, String estado) {
        return evento(id, "checkout.session.completed", """
                {"object":"checkout.session","id":"cs_test_simulada","livemode":false,
                 "payment_status":"%s","subscription":"sub_simulada",
                 "metadata":{"salonId":"%s","tipo":"%s"}}
                """.formatted(estado, salon, tipo), Stripe.API_VERSION);
    }

    private static String evento(String id, String tipo, String objeto, String version) {
        return """
                {"id":"%s","object":"event","type":"%s","api_version":"%s","livemode":false,
                 "data":{"object":%s}}
                """.formatted(id, tipo, version, objeto);
    }

    private String nuevoEvento() {
        String id = "evt_test_" + UUID.randomUUID();
        eventos.add(id);
        return id;
    }

    private Long crearSalon() {
        Long id = new TransactionTemplate(transactionManager).execute(estado -> {
            Salon salon = new Salon();
            salon.setNombre("Salon webhook");
            salon.setSlug("webhook-" + UUID.randomUUID());
            salon.setPlan(Plan.GRATIS);
            salon.setCreditosIa(10);
            salon.setFechaCreacion(LocalDateTime.now(clock));
            entityManager.persist(salon);
            return salon.getId();
        });
        salones.add(id);
        return id;
    }

    private int creditos(Long id) {
        return jdbc.queryForObject("select creditos_ia from salones where id = ?", Integer.class, id);
    }

    private String plan(Long id) {
        return jdbc.queryForObject("select plan from salones where id = ?", String.class, id);
    }

    private int contarEvento(String id) {
        return jdbc.queryForObject("select count(*) from eventos_stripe where id = ?", Integer.class, id);
    }
}
