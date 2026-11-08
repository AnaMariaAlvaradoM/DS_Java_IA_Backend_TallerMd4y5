package com.devsenior.sillon.pago;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "sillon.stripe.secreto-webhook=whsec_prueba_ficticia")
@Transactional
class PagosIntegracionTests {
    private static final String SECRETO_PRUEBA = "whsec_prueba_ficticia";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean CheckoutGateway checkout;

    @Test
    void checkoutWebhookIdempotenciaPermisosYAislamiento() throws Exception {
        JsonNode duenoA = registrar("Pagos A");
        JsonNode duenoB = registrar("Pagos B");
        String tokenA = duenoA.get("token").asString();
        String tokenB = duenoB.get("token").asString();
        long salonA = duenoA.get("salonId").asLong();
        long salonB = duenoB.get("salonId").asLong();
        when(checkout.crear(any())).thenReturn(new SesionCheckout("cs_test_ficticia",
                "https://checkout.stripe.com/ficticia"));

        mvc.perform(post("/api/pagos/suscripcion").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("SUSCRIPCION_PRO"))
                .andExpect(jsonPath("$.sesionId").value("cs_test_ficticia"))
                .andExpect(jsonPath("$.urlPago").value("https://checkout.stripe.com/ficticia"));
        mvc.perform(post("/api/pagos/creditos").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tipo").value("PAQUETE_CREDITOS"));
        mvc.perform(get("/api/pagos/resultado").param("estado", "exito"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mensaje").isString());
        mvc.perform(get("/api/pagos/resultado").param("estado", "cancelado"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/pagos/resultado").param("estado", "otro"))
                .andExpect(status().isBadRequest());

        String correoEstilista = "estilista-" + UUID.randomUUID() + "@ejemplo.co";
        long servicioId = crearServicio(tokenA);
        mvc.perform(post("/api/estilistas").header("Authorization", bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Mateo\",\"email\":\"" + correoEstilista
                        + "\",\"password\":\"estilista123\",\"servicioIds\":[" + servicioId + "]}"))
                .andExpect(status().isCreated());
        String tokenEstilista = login(correoEstilista);
        mvc.perform(post("/api/pagos/suscripcion").header("Authorization", bearer(tokenEstilista)))
                .andExpect(status().isForbidden());

        String sinPagar = eventoCheckout("evt_sin_pagar_ficticio", salonA, "SUSCRIPCION_PRO", "sub_sin_pagar")
                .replace("\"payment_status\":\"paid\"", "\"payment_status\":\"unpaid\"");
        webhook(sinPagar, firmar(sinPagar)).andExpect(status().isOk())
                .andExpect(jsonPath("$.procesado").value(false));
        mvc.perform(get("/api/salon").header("Authorization", bearer(tokenA)))
                .andExpect(jsonPath("$.plan").value("GRATIS"))
                .andExpect(jsonPath("$.creditosIa").value(10));

        String pro = eventoCheckout("evt_pro_ficticio", salonA, "SUSCRIPCION_PRO", "sub_ficticia_1");
        webhook(pro, firmar(pro)).andExpect(status().isOk())
                .andExpect(jsonPath("$.recibido").value(true))
                .andExpect(jsonPath("$.procesado").value(true));
        mvc.perform(get("/api/salon").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.plan").value("PRO"))
                .andExpect(jsonPath("$.creditosIa").value(310));
        mvc.perform(get("/api/salon").header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.plan").value("GRATIS"))
                .andExpect(jsonPath("$.creditosIa").value(10));
        webhook(pro, firmar(pro)).andExpect(status().isOk())
                .andExpect(jsonPath("$.procesado").value(false));
        mvc.perform(get("/api/salon").header("Authorization", bearer(tokenA)))
                .andExpect(jsonPath("$.creditosIa").value(310));
        mvc.perform(post("/api/pagos/suscripcion").header("Authorization", bearer(tokenA)))
                .andExpect(status().isConflict());

        String paquete = eventoCheckout("evt_paquete_ficticio", salonB, "PAQUETE_CREDITOS", null);
        webhook(paquete, firmar(paquete)).andExpect(status().isOk())
                .andExpect(jsonPath("$.procesado").value(true));
        mvc.perform(get("/api/salon").header("Authorization", bearer(tokenB)))
                .andExpect(jsonPath("$.plan").value("GRATIS"))
                .andExpect(jsonPath("$.creditosIa").value(210));
        mvc.perform(get("/api/salon").header("Authorization", bearer(tokenA)))
                .andExpect(jsonPath("$.creditosIa").value(310));

        String cancelacion = """
                {"id":"evt_cancelacion_ficticia","object":"event","type":"customer.subscription.deleted",
                 "data":{"object":{"id":"sub_ficticia_1","object":"subscription"}}}
                """;
        webhook(cancelacion, firmar(cancelacion)).andExpect(status().isOk())
                .andExpect(jsonPath("$.procesado").value(true));
        mvc.perform(get("/api/salon").header("Authorization", bearer(tokenA)))
                .andExpect(jsonPath("$.plan").value("GRATIS"))
                .andExpect(jsonPath("$.creditosIa").value(310));
        webhook(cancelacion, firmar(cancelacion)).andExpect(status().isOk())
                .andExpect(jsonPath("$.procesado").value(false));
        webhook(pro, "t=1,v1=firma_falsa").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400));
        mvc.perform(post("/api/pagos/webhook").contentType(MediaType.APPLICATION_JSON).content(pro))
                .andExpect(status().isBadRequest());
    }

    private String eventoCheckout(String id, long salonId, String tipoCompra, String suscripcionId) {
        String suscripcion = suscripcionId == null ? "null" : "\"" + suscripcionId + "\"";
        return """
                {"id":"%s","object":"event","type":"checkout.session.completed",
                 "data":{"object":{"id":"cs_test_ficticia","object":"checkout.session",
                 "payment_status":"paid","subscription":%s,
                 "metadata":{"salonId":"%d","tipoCompra":"%s"}}}}
                """.formatted(id, suscripcion, salonId, tipoCompra);
    }

    private org.springframework.test.web.servlet.ResultActions webhook(String cuerpo, String firma) throws Exception {
        return mvc.perform(post("/api/pagos/webhook").header("Stripe-Signature", firma)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private String firmar(String cuerpo) throws Exception {
        long tiempo = Instant.now().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRETO_PRUEBA.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] firma = mac.doFinal((tiempo + "." + cuerpo).getBytes(StandardCharsets.UTF_8));
        return "t=" + tiempo + ",v1=" + HexFormat.of().formatHex(firma);
    }

    private JsonNode registrar(String nombre) throws Exception {
        String correo = "dueno-" + UUID.randomUUID() + "@ejemplo.co";
        String cuerpo = """
                {"salonNombre":"%s","ciudad":"Bogotá","direccion":"Calle 1","telefono":"1234567",
                 "nombre":"Dueño","email":"%s","password":"secreta123"}
                """.formatted(nombre, correo);
        MvcResult resultado = mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(resultado.getResponse().getContentAsString());
    }

    private long crearServicio(String token) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/servicios").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Corte\",\"duracionMinutos\":45,\"precio\":35000}"))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private String login(String correo) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + correo + "\",\"password\":\"estilista123\"}"))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(resultado.getResponse().getContentAsString()).get("token").asString();
    }

    private String bearer(String token) { return "Bearer " + token; }
}
