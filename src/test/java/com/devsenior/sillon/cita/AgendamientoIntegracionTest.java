package com.devsenior.sillon.cita;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.seguridad.ServicioJwt;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
class AgendamientoIntegracionTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ServicioJwt jwt;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private Clock clock;

    private final List<Long> salonesDePrueba = new ArrayList<>();
    private LocalDate lunes;
    private Datos propios;

    @BeforeEach
    void preparar() {
        lunes = LocalDate.now(clock).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        propios = crearDatos();
    }

    @AfterEach
    void limpiar() {
        for (Long salonId : salonesDePrueba) {
            jdbc.update("delete from citas where salon_id = ?", salonId);
            jdbc.update("delete from clientes where salon_id = ?", salonId);
            jdbc.update("delete from estilista_servicios where estilista_id in (select id from usuarios where salon_id = ?)",
                    salonId);
            jdbc.update("delete from usuarios where salon_id = ?", salonId);
            jdbc.update("delete from servicios where salon_id = ?", salonId);
            jdbc.update("delete from salones where id = ?", salonId);
        }
    }

    @Test
    void agendaConLosCamposDelContratoYAceptaUnaCitaConsecutiva() throws Exception {
        agendar(propios, propios.servicioId, propios.estilistaId, lunes + "T10:00:00", "300-123-4567", "")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(15))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.inicio").value(lunes + "T10:00:00"))
                .andExpect(jsonPath("$.fin").value(lunes + "T10:45:00"))
                .andExpect(jsonPath("$.precio").value(35000))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.origen").value("SALON"))
                .andExpect(jsonPath("$.servicioId").value(propios.servicioId))
                .andExpect(jsonPath("$.servicioNombre").value("Corte"))
                .andExpect(jsonPath("$.estilistaId").value(propios.estilistaId))
                .andExpect(jsonPath("$.estilistaNombre").value("Mateo"))
                .andExpect(jsonPath("$.clienteNombre").value("Sara"))
                .andExpect(jsonPath("$.clienteTelefono").value("3001234567"))
                .andExpect(jsonPath("$.nota").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.fechaCreacion").isString())
                .andExpect(jsonPath("$.fechaActualizacion").isString());

        agendar(propios, "10:30:00").andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value(409));
        agendar(propios, "10:45:00").andExpect(status().isCreated());
        assertThat(contar("clientes", propios)).isEqualTo(1);
        assertThat(contar("citas", propios)).isEqualTo(2);
    }

    @Test
    void pendienteBloqueaPeroCanceladaNoOcupaAgenda() throws Exception {
        agendar(propios, "10:00:00").andExpect(status().isCreated());
        jdbc.update("update citas set estado = 'PENDIENTE' where salon_id = ?", propios.salonId);
        agendar(propios, "10:15:00").andExpect(status().isConflict());
        jdbc.update("update citas set estado = 'CANCELADA' where salon_id = ?", propios.salonId);
        agendar(propios, "10:15:00").andExpect(status().isCreated());
    }

    @Test
    void noPermiteAsociarServicioNiEstilistaDeOtroSalon() throws Exception {
        Datos ajenos = crearDatos();
        agendar(propios, ajenos.servicioId, propios.estilistaId, lunes + "T10:00:00", "3001234567",
                ",\"salonId\":" + ajenos.salonId).andExpect(status().isNotFound());
        agendar(propios, propios.servicioId, ajenos.estilistaId, lunes + "T10:00:00", "3001234567", "")
                .andExpect(status().isNotFound());
        assertThat(contar("clientes", propios)).isZero();
        agendar(propios, "10:00:00").andExpect(status().isCreated());
        agendar(ajenos, "10:00:00").andExpect(status().isCreated());
        assertThat(contar("citas", propios)).isEqualTo(1);
        assertThat(contar("citas", ajenos)).isEqualTo(1);
    }

    @Test
    void rechazaEstilistaQueNoOfreceElServicioOEstaInactivo() throws Exception {
        jdbc.update("delete from estilista_servicios where estilista_id = ?", propios.estilistaId);
        agendar(propios, "10:00:00").andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(Matchers.containsString("ofrece")));
        jdbc.update("update usuarios set activo = false where id = ?", propios.estilistaId);
        agendar(propios, "10:00:00").andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(Matchers.containsString("inactivo")));
        jdbc.update("update servicios set activo = false where id = ?", propios.servicioId);
        agendar(propios, "10:00:00").andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(Matchers.containsString("servicio")));
        assertThat(contar("clientes", propios)).isZero();
    }

    @Test
    void responde400PorDatosInvalidosY409PorPasadoDomingoOFueraDeHorario() throws Exception {
        agendar(propios, propios.servicioId, propios.estilistaId, lunes + "T10:00:00", "123", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(Matchers.containsString("clienteTelefono")));
        agendar(propios, propios.servicioId, propios.estilistaId, "fecha-invalida", "3001234567", "")
                .andExpect(status().isBadRequest());
        agendar(propios, propios.servicioId, propios.estilistaId,
                LocalDate.now(clock).minusDays(1) + "T10:00:00", "3001234567", "")
                .andExpect(status().isConflict());
        agendar(propios, propios.servicioId, propios.estilistaId, lunes.plusDays(6) + "T10:00:00", "3001234567", "")
                .andExpect(status().isConflict());
        agendar(propios, "18:45:00").andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(Matchers.containsString("08:00 a 19:00")));
        assertThat(contar("clientes", propios)).isZero();
    }

    @Test
    void soloElDuenoPuedeAgendar() throws Exception {
        mockMvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/citas").header("Authorization", "Bearer " + propios.tokenEstilista)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    void dosSolicitudesSimultaneasSoloGuardanUnaCitaEnMySql() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var barrera = new CyclicBarrier(2);
        try {
            var primera = executor.submit(() -> {
                barrera.await(10, TimeUnit.SECONDS);
                return agendar(propios, "10:00:00").andReturn().getResponse().getStatus();
            });
            var segunda = executor.submit(() -> {
                barrera.await(10, TimeUnit.SECONDS);
                return agendar(propios, "10:15:00").andReturn().getResponse().getStatus();
            });

            assertThat(List.of(primera.get(20, TimeUnit.SECONDS), segunda.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
            assertThat(contar("citas", propios)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(25, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void reservaSinTokenQuedaPendienteYOcupaAgenda() throws Exception {
        reservar(propios.slug, propios.servicioId, propios.estilistaId, lunes + "T10:00:00", "300-123-4567", "")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(15))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.origen").value("EN_LINEA"))
                .andExpect(jsonPath("$.inicio").value(lunes + "T10:00:00"))
                .andExpect(jsonPath("$.fin").value(lunes + "T10:45:00"))
                .andExpect(jsonPath("$.precio").value(35000))
                .andExpect(jsonPath("$.clienteTelefono").value("3001234567"));

        mockMvc.perform(get("/api/publico/salones/{slug}/disponibilidad", propios.slug)
                .param("servicioId", Long.toString(propios.servicioId))
                .param("estilistaId", Long.toString(propios.estilistaId))
                .param("fecha", lunes.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horariosLibres").value(Matchers.not(Matchers.hasItem("09:30:00"))))
                .andExpect(jsonPath("$.horariosLibres").value(Matchers.not(Matchers.hasItem("10:00:00"))))
                .andExpect(jsonPath("$.horariosLibres").value(Matchers.not(Matchers.hasItem("10:30:00"))))
                .andExpect(jsonPath("$.horariosLibres").value(Matchers.hasItem("11:00:00")));
        agendar(propios, "10:30:00").andExpect(status().isConflict());
    }

    @Test
    void reservaRechazaDatosInvalidosYSoloUsaElSalonDelSlug() throws Exception {
        Datos ajenos = crearDatos();
        String inicio = lunes + "T10:00:00";
        reservar(propios.slug, propios.servicioId, propios.estilistaId, inicio, "123", "")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value(400));
        reservar("inexistente-" + UUID.randomUUID(), propios.servicioId, propios.estilistaId, inicio, "3001234567", "")
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value(404));
        reservar(propios.slug, ajenos.servicioId, propios.estilistaId, inicio, "3001234567",
                ",\"salonId\":" + ajenos.salonId).andExpect(status().isNotFound());
        reservar(propios.slug, propios.servicioId, ajenos.estilistaId, inicio, "3001234567", "")
                .andExpect(status().isNotFound());
        assertThat(contar("citas", propios)).isZero();
        assertThat(contar("citas", ajenos)).isZero();
    }

    @Test
    void reservaReutilizaLasValidacionesDeHorarioFechaYServicio() throws Exception {
        reservar(propios.slug, propios.servicioId, propios.estilistaId, lunes + "T18:45:00", "3001234567", "")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje", Matchers.containsString("19:00")));
        reservar(propios.slug, propios.servicioId, propios.estilistaId,
                LocalDate.now(clock).minusDays(1) + "T10:00:00", "3001234567", "")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje", Matchers.containsString("pasado")));
        jdbc.update("update servicios set activo = false where id = ?", propios.servicioId);
        reservar(propios.slug, propios.servicioId, propios.estilistaId, lunes + "T10:00:00", "3001234567", "")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje", Matchers.containsString("inactivo")));
        assertThat(contar("clientes", propios)).isZero();
    }

    @Test
    void reservaPublicaYCitaInternaSimultaneasSoloGuardanUnaCitaEnMySql() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var barrera = new CyclicBarrier(2);
        try {
            var publica = executor.submit(() -> {
                barrera.await(10, TimeUnit.SECONDS);
                return reservar(propios.slug, propios.servicioId, propios.estilistaId,
                        lunes + "T10:00:00", "3001234568", "").andReturn().getResponse().getStatus();
            });
            var interna = executor.submit(() -> {
                barrera.await(10, TimeUnit.SECONDS);
                return agendar(propios, "10:00:00").andReturn().getResponse().getStatus();
            });

            int estadoPublica = publica.get(20, TimeUnit.SECONDS);
            assertThat(List.of(estadoPublica, interna.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
            assertThat(contar("citas", propios)).isEqualTo(1);
            assertThat(contar("clientes", propios)).isEqualTo(1);
            assertThat(jdbc.queryForMap("select estado, origen from citas where salon_id = ?", propios.salonId))
                    .containsEntry("estado", estadoPublica == 201 ? "PENDIENTE" : "CONFIRMADA")
                    .containsEntry("origen", estadoPublica == 201 ? "EN_LINEA" : "SALON");
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(25, TimeUnit.SECONDS)).isTrue();
        }
    }

    private ResultActions reservar(String slug, long servicioId, long estilistaId, String inicio, String telefono,
            String camposExtra) throws Exception {
        return mockMvc.perform(post("/api/publico/salones/{slug}/reservas", slug)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoReserva(servicioId, estilistaId, inicio, telefono, camposExtra)));
    }

    private int contar(String tabla, Datos datos) {
        return jdbc.queryForObject("select count(*) from " + tabla + " where salon_id = ?", Integer.class, datos.salonId);
    }

    private ResultActions agendar(Datos datos, String hora) throws Exception {
        return agendar(datos, datos.servicioId, datos.estilistaId, lunes + "T" + hora, "3001234567", "");
    }

    private ResultActions agendar(Datos datos, long servicioId, long estilistaId, String inicio, String telefono,
            String camposExtra) throws Exception {
        return mockMvc.perform(post("/api/citas").header("Authorization", "Bearer " + datos.token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoReserva(servicioId, estilistaId, inicio, telefono, camposExtra)));
    }

    private static String cuerpoReserva(long servicioId, long estilistaId, String inicio, String telefono,
            String camposExtra) {
        return """
                {"clienteNombre":"Sara","clienteTelefono":"%s","servicioId":%d,
                 "estilistaId":%d,"inicio":"%s"%s}
                """.formatted(telefono, servicioId, estilistaId, inicio, camposExtra);
    }

    private Datos crearDatos() {
        Datos datos = new TransactionTemplate(transactionManager).execute(estado -> {
            Salon salon = new Salon();
            salon.setNombre("Salon de prueba");
            salon.setSlug("agenda-" + UUID.randomUUID());
            salon.setPlan(Plan.GRATIS);
            salon.setConfiguracion(ConfiguracionSalon.porDefecto());
            salon.setFechaCreacion(lunes.atStartOfDay());
            entityManager.persist(salon);

            Servicio servicio = new Servicio();
            servicio.setSalon(salon);
            servicio.setNombre("Corte");
            servicio.setDuracionMinutos(45);
            servicio.setPrecio(35000);
            servicio.setActivo(true);
            entityManager.persist(servicio);

            Usuario dueno = crearUsuario(salon, "Dueno", Rol.DUENO);
            Usuario estilista = crearUsuario(salon, "Mateo", Rol.ESTILISTA);
            estilista.setServicios(Set.of(servicio));
            return new Datos(salon.getId(), salon.getSlug(), servicio.getId(), estilista.getId(),
                    jwt.generarToken(dueno.getEmail(), Rol.DUENO.name(), salon.getId()),
                    jwt.generarToken(estilista.getEmail(), Rol.ESTILISTA.name(), salon.getId()));
        });
        salonesDePrueba.add(datos.salonId);
        return datos;
    }

    private Usuario crearUsuario(Salon salon, String nombre, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setSalon(salon);
        usuario.setNombre(nombre);
        usuario.setEmail(UUID.randomUUID() + "@agenda.example");
        usuario.setPassword("valor-no-usado-para-autenticacion");
        usuario.setRol(rol);
        usuario.setActivo(true);
        entityManager.persist(usuario);
        return usuario;
    }

    private record Datos(long salonId, String slug, long servicioId, long estilistaId, String token, String tokenEstilista) {
    }
}
