package com.devsenior.sillon.pago;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.devsenior.sillon.auth.AuthResponse;
import com.devsenior.sillon.auth.AuthService;
import com.devsenior.sillon.auth.LoginRequest;
import com.devsenior.sillon.auth.RegistroRequest;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.UsuarioRepository;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {"sillon.stripe.clave-secreta=sk_test_simulador_local",
        "sillon.stripe.secreto-webhook=whsec_solo_para_pruebas", "sillon.stripe.precio-pro=price_pro_prueba",
        "sillon.stripe.precio-creditos=price_creditos_prueba"})
@AutoConfigureMockMvc
class PagosWebhookIntegracionTest {
    private static final String FIRMA_PRUEBA = "whsec_solo_para_pruebas";
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private AuthService auth;
    @Autowired private SalonRepository salones;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private Clock reloj;
    @MockitoBean private PasarelaStripe pasarela;
    @MockitoSpyBean private EventoStripeRepository eventos;
    private final List<String> idsEventos = new ArrayList<>();
    private AuthResponse dueno;

    @BeforeEach void preparar() {
        dueno = auth.registrar(new RegistroRequest("Pagos " + UUID.randomUUID(), "Bogotá", "Calle 1", "3001112233",
                "Dueño", "pago." + UUID.randomUUID() + "@ejemplo.test", "secreta123"));
        Session sesion = new Session(); sesion.setId("cs_test_simulada"); sesion.setUrl("https://checkout.stripe.test/pago");
        when(pasarela.crearSesion(any())).thenReturn(sesion);
    }

    @AfterEach void limpiar() {
        reset(eventos);
        for (String id : idsEventos) { eventos.deleteById(id); }
        if (dueno != null) { usuarios.deleteById(dueno.usuarioId()); salones.deleteById(dueno.salonId()); }
    }

    @Test void crearSesionNoActivaElPlanNiEntregaCreditos() throws Exception {
        mvc.perform(post("/api/pagos/suscripcion").header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tipo").value("SUSCRIPCION_PRO"))
                .andExpect(jsonPath("$.sesionId").value("cs_test_simulada"))
                .andExpect(jsonPath("$.urlPago").value(org.hamcrest.Matchers.startsWith("https://")));
        mvc.perform(post("/api/pagos/creditos").header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tipo").value("PAQUETE_CREDITOS"));
        assertEquals(Plan.GRATIS, salon().getPlan()); assertEquals(10, salon().getCreditosIa());
    }

    @Test void firmaFaltanteOFalsaEs400() throws Exception {
        mvc.perform(post("/api/pagos/webhook").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value(400));
        mvc.perform(post("/api/pagos/webhook").contentType(MediaType.APPLICATION_JSON).content("{}")
                        .header("Stripe-Signature", "t=1,v1=falsa"))
                .andExpect(status().isBadRequest());
    }

    @Test void proSuma300GuardaSuscripcionYNoDuplicaEvento() throws Exception {
        String cuerpo = evento("checkout.session.completed", compra(TipoCompra.SUSCRIPCION_PRO, "paid"), Stripe.API_VERSION);
        enviar(cuerpo).andExpect(status().isOk()).andExpect(jsonPath("$.recibido").value(true))
                .andExpect(jsonPath("$.procesado").value(true));
        assertEquals(Plan.PRO, salon().getPlan()); assertEquals(310, salon().getCreditosIa());
        assertEquals(suscripcion(), salon().getStripeSuscripcionId());
        enviar(cuerpo).andExpect(status().isOk()).andExpect(jsonPath("$.procesado").value(false));
        assertEquals(310, salon().getCreditosIa());
    }

    @Test void paqueteSuma200SinCambiarPlan() throws Exception {
        enviar(evento("checkout.session.completed", compra(TipoCompra.PAQUETE_CREDITOS, "paid"), Stripe.API_VERSION))
                .andExpect(status().isOk());
        assertEquals(Plan.GRATIS, salon().getPlan()); assertEquals(210, salon().getCreditosIa());
    }

    @Test void cancelarSuscripcionRegresaAGratisYConservaCreditos() throws Exception {
        enviar(evento("checkout.session.completed", compra(TipoCompra.SUSCRIPCION_PRO, "paid"), Stripe.API_VERSION))
                .andExpect(status().isOk());
        enviar(evento("customer.subscription.deleted", Map.of("id", suscripcion(), "object", "subscription"), Stripe.API_VERSION))
                .andExpect(status().isOk());
        assertEquals(Plan.GRATIS, salon().getPlan()); assertEquals(310, salon().getCreditosIa());
    }

    @Test void yaProResponde409SinCrearOtraSesion() throws Exception {
        enviar(evento("checkout.session.completed", compra(TipoCompra.SUSCRIPCION_PRO, "paid"), Stripe.API_VERSION))
                .andExpect(status().isOk());
        mvc.perform(post("/api/pagos/suscripcion").header("Authorization", bearer()))
                .andExpect(status().isConflict());
        verifyNoInteractions(pasarela);
    }

    @Test void pagoNoConfirmadoSeRegistraSinEntregarBeneficios() throws Exception {
        String cuerpo = evento("checkout.session.completed", compra(TipoCompra.SUSCRIPCION_PRO, "unpaid"), Stripe.API_VERSION);
        enviar(cuerpo).andExpect(status().isOk()).andExpect(jsonPath("$.procesado").value(true));
        assertEquals(Plan.GRATIS, salon().getPlan()); assertEquals(10, salon().getCreditosIa());
        enviar(cuerpo).andExpect(status().isOk()).andExpect(jsonPath("$.procesado").value(false));
    }

    @Test void otroTipoFirmadoSeRegistraUnaSolaVez() throws Exception {
        String cuerpo = evento("customer.created", Map.of("id", "cus_test", "object", "customer"), Stripe.API_VERSION);
        enviar(cuerpo).andExpect(status().isOk()).andExpect(jsonPath("$.procesado").value(true));
        enviar(cuerpo).andExpect(status().isOk()).andExpect(jsonPath("$.procesado").value(false));
        assertEquals(10, salon().getCreditosIa());
    }

    @Test void versionDistintaUsaFallbackDeLaLibreria() throws Exception {
        enviar(evento("checkout.session.completed", compra(TipoCompra.SUSCRIPCION_PRO, "paid"), "2020-08-27"))
                .andExpect(status().isOk());
        assertEquals(Plan.PRO, salon().getPlan());
    }

    @Test void metadataInvalidaNoRegistraEventoNiCambiaElSalon() throws Exception {
        Map<String, Object> compra = compra(TipoCompra.SUSCRIPCION_PRO, "paid"); compra.put("metadata", Map.of());
        enviar(evento("checkout.session.completed", compra, Stripe.API_VERSION)).andExpect(status().isBadGateway());
        assertFalse(eventos.existsById(idsEventos.get(0)));
        assertEquals(Plan.GRATIS, salon().getPlan()); assertEquals(10, salon().getCreditosIa());
    }

    @Test void falloAlGuardarEventoRevierteLosCambiosDelSalon() throws Exception {
        String cuerpo = evento("checkout.session.completed", compra(TipoCompra.SUSCRIPCION_PRO, "paid"), Stripe.API_VERSION);
        doThrow(new DataIntegrityViolationException("Fallo simulado de almacenamiento"))
                .when(eventos).saveAndFlush(any(EventoStripe.class));
        enviar(cuerpo).andExpect(status().isConflict());
        assertFalse(eventos.existsById(idsEventos.get(0)));
        assertEquals(Plan.GRATIS, salon().getPlan()); assertEquals(10, salon().getCreditosIa());
    }

    @Test void estilistaNoPuedePagar() throws Exception {
        var usuario = usuarios.findById(dueno.usuarioId()).orElseThrow(); usuario.setRol(Rol.ESTILISTA);
        usuarios.saveAndFlush(usuario);
        AuthResponse estilista = auth.login(new LoginRequest(usuario.getEmail(), "secreta123"));
        mvc.perform(post("/api/pagos/suscripcion").header("Authorization", "Bearer " + estilista.token()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(pasarela);
    }

    @Test void resultadoEsPublicoYNoConfirmaElPago() throws Exception {
        mvc.perform(get("/api/pagos/resultado").param("estado", "exito"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mensaje").isNotEmpty());
        mvc.perform(get("/api/pagos/resultado").param("estado", "cancelado"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/pagos/resultado").param("estado", "otro"))
                .andExpect(status().isBadRequest());
        assertEquals(Plan.GRATIS, salon().getPlan());
    }

    private Map<String, Object> compra(TipoCompra tipo, String estado) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("id", "cs_test_" + UUID.randomUUID()); datos.put("object", "checkout.session");
        datos.put("payment_status", estado);
        datos.put("metadata", Map.of("salonId", dueno.salonId().toString(), "tipoCompra", tipo.name()));
        if (tipo == TipoCompra.SUSCRIPCION_PRO) { datos.put("subscription", suscripcion()); }
        return datos;
    }
    private String evento(String tipo, Map<String, ?> objeto, String version) throws Exception {
        String id = "evt_test_" + UUID.randomUUID(); idsEventos.add(id);
        return mapper.writeValueAsString(Map.of("id", id, "object", "event", "type", tipo, "livemode", false,
                "api_version", version, "data", Map.of("object", objeto)));
    }
    private org.springframework.test.web.servlet.ResultActions enviar(String cuerpo) throws Exception {
        long tiempo = reloj.instant().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(FIRMA_PRUEBA.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String firma = HexFormat.of().formatHex(mac.doFinal((tiempo + "." + cuerpo).getBytes(StandardCharsets.UTF_8)));
        return mvc.perform(post("/api/pagos/webhook").contentType(MediaType.APPLICATION_JSON).content(cuerpo)
                .header("Stripe-Signature", "t=" + tiempo + ",v1=" + firma));
    }
    private String suscripcion() { return "sub_test_" + dueno.salonId(); }
    private String bearer() { return "Bearer " + dueno.token(); }
    private Salon salon() { return salones.findById(dueno.salonId()).orElseThrow(); }
}
