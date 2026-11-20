package com.devsenior.sillon.pago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.seguridad.ServicioJwt;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.stripe.StripeClient;
import com.stripe.net.HttpClient;
import com.stripe.net.HttpHeaders;
import com.stripe.net.StripeResponse;

import jakarta.persistence.EntityManager;

@SpringBootTest(properties = "sillon.stripe.clave-secreta=sk_test_simulada")
@AutoConfigureMockMvc
@Transactional
@Import(PagoIntegracionTest.StripeSimulado.class)
class PagoIntegracionTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private ServicioJwt jwt;
    @Autowired private Clock clock;
    @Autowired private HttpClient transporte;

    private Salon salon;
    private String token;

    @BeforeEach
    void preparar() {
        org.mockito.Mockito.reset(transporte);
        salon = new Salon();
        salon.setNombre("Salon de prueba");
        salon.setSlug("pagos-" + UUID.randomUUID());
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(10);
        salon.setFechaCreacion(LocalDateTime.now(clock));
        entityManager.persist(salon);
        token = token(Rol.DUENO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"suscripcion", "creditos"})
    void devuelveContratoExactoSinModificarPlanNiCreditos(String ruta) throws Exception {
        given(transporte.requestWithRetries(any())).willReturn(new StripeResponse(200, HttpHeaders.of(Map.of()), """
                {"object":"checkout.session","id":"cs_test_simulada",
                 "url":"https://checkout.stripe.com/prueba","livemode":false}
                """));
        mockMvc.perform(post("/api/pagos/" + ruta).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"precio\":\"price_cliente\",\"cantidad\":900,\"salonId\":-1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$.tipo").value(ruta.equals("suscripcion") ? "SUSCRIPCION_PRO" : "PAQUETE_CREDITOS"))
                .andExpect(jsonPath("$.sesionId").value("cs_test_simulada"))
                .andExpect(jsonPath("$.urlPago").value("https://checkout.stripe.com/prueba"));
        entityManager.flush();
        entityManager.clear();
        Salon persistido = entityManager.find(Salon.class, salon.getId());
        assertThat(persistido.getPlan()).isEqualTo(Plan.GRATIS);
        assertThat(persistido.getCreditosIa()).isEqualTo(10);
        assertThat(persistido.getStripeSuscripcionId()).isNull();
    }

    @Test
    void proNoPuedeVolverAContratarSinContactarStripe() throws Exception {
        salon.setPlan(Plan.PRO);
        mockMvc.perform(post("/api/pagos/suscripcion").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value(409));
        verifyNoInteractions(transporte);
    }

    @ParameterizedTest
    @ValueSource(strings = {"suscripcion", "creditos"})
    void sinTokenDevuelve401YEstilistaOAdminReciben403(String ruta) throws Exception {
        mockMvc.perform(post("/api/pagos/" + ruta))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value(401));
        for (Rol rol : new Rol[] {Rol.ESTILISTA, Rol.ADMIN}) {
            mockMvc.perform(post("/api/pagos/" + ruta).header("Authorization", "Bearer " + token(rol)))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value(403));
        }
        verifyNoInteractions(transporte);
    }

    @Test
    void sesionInvalidaDevuelve502ConFormatoCompleto() throws Exception {
        given(transporte.requestWithRetries(any())).willReturn(new StripeResponse(200, HttpHeaders.of(Map.of()), "{}"));
        mockMvc.perform(post("/api/pagos/creditos").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$.codigo").value(502))
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(jsonPath("$.mensaje").isString())
                .andExpect(jsonPath("$.fecha").isString());
    }

    private String token(Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setSalon(salon);
        usuario.setNombre("Usuario de prueba");
        usuario.setEmail(UUID.randomUUID() + "@pagos.example");
        usuario.setPassword("valor-no-usado-para-autenticacion");
        usuario.setRol(rol);
        usuario.setActivo(true);
        entityManager.persist(usuario);
        return jwt.generarToken(usuario.getEmail(), rol.name(), salon.getId());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class StripeSimulado {
        @Bean
        HttpClient transporteStripeSimulado() {
            return mock(HttpClient.class);
        }

        @Bean
        @Primary
        StripeClient clienteStripeSimulado(HttpClient transporte) {
            return StripeClient.builder().setApiKey("sk_test_simulada")
                    .setApiBase("http://127.0.0.1:12345").setHttpClient(transporte).build();
        }
    }
}
