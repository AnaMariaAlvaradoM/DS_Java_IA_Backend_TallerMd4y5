package com.devsenior.sillon.cita;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.seguridad.ServicioJwt;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;

import jakarta.persistence.EntityManager;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservaAuditoriaIntegracionTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDateTime CREACION = LocalDateTime.of(2026, 11, 19, 9, 0, 0, 100_001_000);

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private ServicioJwt jwt;
    @Autowired private ObjectMapper mapper;
    @MockitoBean private Clock clock;

    private Salon salon;
    private Servicio servicio;
    private Usuario estilista;
    private Usuario dueno;

    @BeforeEach
    void preparar() {
        given(clock.getZone()).willReturn(BOGOTA);
        given(clock.instant()).willReturn(CREACION.atZone(BOGOTA).toInstant());
        salon = new Salon();
        salon.setNombre("Salon auditoria");
        salon.setSlug("auditoria-" + UUID.randomUUID());
        salon.setPlan(Plan.GRATIS);
        salon.setConfiguracion(ConfiguracionSalon.porDefecto());
        salon.setFechaCreacion(CREACION);
        entityManager.persist(salon);
        servicio = new Servicio();
        servicio.setSalon(salon);
        servicio.setNombre("Corte");
        servicio.setDuracionMinutos(45);
        servicio.setPrecio(35000);
        servicio.setActivo(true);
        entityManager.persist(servicio);
        dueno = usuario(Rol.DUENO);
        estilista = usuario(Rol.ESTILISTA);
        estilista.getServicios().add(servicio);
        entityManager.flush();
    }

    @Test
    void confirmarReservaConservaEnHttpLosMicrosegundosRealesPersistidos() throws Exception {
        String cuerpo = """
                {"clienteNombre":"Sara","clienteTelefono":"3001234567","servicioId":%d,
                 "estilistaId":%d,"inicio":"2026-11-20T10:30:00"}
                """.formatted(servicio.getId(), estilista.getId());
        var reserva = mockMvc.perform(post("/api/publico/salones/{slug}/reservas", salon.getSlug())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isCreated())
                .andReturn();
        var creada = mapper.readTree(reserva.getResponse().getContentAsString());
        long id = creada.path("id").asLong();
        LocalDateTime actualizacion = CREACION.plusNanos(1000);
        given(clock.instant()).willReturn(actualizacion.atZone(BOGOTA).toInstant());
        String token = jwt.generarToken(dueno.getEmail(), Rol.DUENO.name(), salon.getId());
        var confirmacion = mockMvc.perform(patch("/api/citas/{id}/estado", id)
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"CONFIRMADA\"}")).andExpect(status().isOk()).andReturn();
        var confirmada = mapper.readTree(confirmacion.getResponse().getContentAsString());
        entityManager.clear();
        Cita persistida = entityManager.find(Cita.class, id);

        assertThat(persistida.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        assertThat(persistida.getFechaCreacion()).isEqualTo(CREACION);
        assertThat(persistida.getFechaActualizacion()).isEqualTo(actualizacion);
        assertThat(LocalDateTime.parse(creada.path("fechaCreacion").asString())).isEqualTo(CREACION);
        assertThat(LocalDateTime.parse(confirmada.path("fechaActualizacion").asString())).isEqualTo(actualizacion);
        assertThat(confirmada.path("fechaActualizacion").asString())
                .isGreaterThan(confirmada.path("fechaCreacion").asString());
        assertThat(confirmada.path("fechaCreacion").asString()).isEqualTo(creada.path("fechaCreacion").asString());
        assertThat(creada.path("inicio").asString()).isEqualTo("2026-11-20T10:30:00");
    }

    private Usuario usuario(Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setSalon(salon);
        usuario.setNombre("Usuario");
        usuario.setEmail(UUID.randomUUID() + "@auditoria.example");
        usuario.setPassword("valor-no-usado-para-autenticacion");
        usuario.setRol(rol);
        usuario.setActivo(true);
        entityManager.persist(usuario);
        return usuario;
    }
}
