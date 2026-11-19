package com.devsenior.sillon.pago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.stripe.net.HttpClient;

import jakarta.persistence.EntityManager;

@SpringBootTest(properties = "sillon.stripe.clave-secreta=sk_test_simulada")
@AutoConfigureMockMvc
@Transactional
@Import(PagoIntegracionTest.StripeSimulado.class)
class ResultadoPagoIntegracionTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private EventoStripeRepository eventos;
    @Autowired private Clock clock;
    @Autowired private HttpClient transporte;

    private final List<Salon> salones = new ArrayList<>();
    private String eventoId;
    private LocalDateTime fechaEvento;
    private long eventosPrevios;

    @BeforeEach
    void preparar() {
        org.mockito.Mockito.reset(transporte);
        for (Plan plan : Plan.values()) {
            Salon salon = new Salon();
            salon.setNombre("Salon resultado");
            salon.setSlug("resultado-" + UUID.randomUUID());
            salon.setPlan(plan);
            salon.setCreditosIa(plan == Plan.PRO ? 350 : 10);
            salon.setStripeSuscripcionId(plan == Plan.PRO ? "sub_resultado_previa" : null);
            salon.setFechaCreacion(LocalDateTime.now(clock));
            entityManager.persist(salon);
            salones.add(salon);
        }
        entityManager.flush();
        eventoId = "evt_resultado_" + UUID.randomUUID();
        fechaEvento = LocalDateTime.now(clock).withNano(0);
        eventos.registrar(eventoId, "checkout.session.completed", fechaEvento);
        eventosPrevios = eventos.count();
    }

    @ParameterizedTest
    @CsvSource({"exito,confirmación", "cancelado,cancelado"})
    void resultadoEsPublicoEInformativoSinCambiarBeneficiosNiEventos(String estado, String fragmento) throws Exception {
        mockMvc.perform(get("/api/pagos/resultado").param("estado", estado)
                .param("salonId", salones.get(0).getId().toString())
                .param("creditos", "99999").param("tipo", "SUSCRIPCION_PRO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$.mensaje").value(containsString(fragmento)));
        verificarSinEfectos();
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"EXITO", "CANCELADO", "pendiente", " ", "exito,cancelado", "desconocido"})
    void valoresNoAdmitidosDevuelven400(String estado) throws Exception {
        mockMvc.perform(get("/api/pagos/resultado").param("estado", estado))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensaje").value(containsString("estado")))
                .andExpect(jsonPath("$.fecha").isString());
        verificarSinEfectos();
    }

    @Test
    void estadoAusenteDevuelve400() throws Exception {
        mockMvc.perform(get("/api/pagos/resultado"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje").value(containsString("estado")));
        verificarSinEfectos();
    }

    @Test
    void estadosContradictoriosRepetidosDevuelven400() throws Exception {
        mockMvc.perform(get("/api/pagos/resultado").param("estado", "exito", "cancelado"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value(400));
        verificarSinEfectos();
    }

    private void verificarSinEfectos() {
        entityManager.flush();
        entityManager.clear();
        for (Salon esperado : salones) {
            assertThat(entityManager.find(Salon.class, esperado.getId()))
                    .extracting(Salon::getPlan, Salon::getCreditosIa, Salon::getStripeSuscripcionId)
                    .containsExactly(esperado.getPlan(), esperado.getCreditosIa(), esperado.getStripeSuscripcionId());
        }
        assertThat(eventos.count()).isEqualTo(eventosPrevios);
        EventoStripe evento = eventos.findById(eventoId).orElseThrow();
        assertThat(evento.getTipo()).isEqualTo("checkout.session.completed");
        assertThat(evento.getFechaProcesado()).isEqualTo(fechaEvento);
        verifyNoInteractions(transporte);
    }
}
