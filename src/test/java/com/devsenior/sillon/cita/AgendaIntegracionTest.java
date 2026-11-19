package com.devsenior.sillon.cita;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.seguridad.ServicioJwt;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.jayway.jsonpath.JsonPath;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgendaIntegracionTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ServicioJwt jwt;
    @Autowired private Clock clock;

    private LocalDate hoy;
    private Equipo equipo;

    @BeforeEach
    void preparar() {
        hoy = LocalDate.now(clock);
        equipo = crearEquipo();
    }

    @Test
    void listaHoyPorDefectoYOrdenaSinIncluirOtrosSalonesNiOtrosDias() throws Exception {
        Cita tarde = crearCita(equipo, equipo.otroEstilista(), hoy.atTime(15, 0), EstadoCita.CANCELADA);
        Cita primera = crearCita(equipo, equipo.estilista(), hoy.atStartOfDay(), EstadoCita.CONFIRMADA);
        crearCita(equipo, equipo.estilista(), hoy.minusDays(1).atTime(23, 30), EstadoCita.CONFIRMADA);
        Cita manana = crearCita(equipo, equipo.estilista(), hoy.plusDays(1).atStartOfDay(), EstadoCita.CONFIRMADA);
        Equipo ajeno = crearEquipo();
        crearCita(ajeno, ajeno.estilista(), hoy.atTime(9, 0), EstadoCita.CONFIRMADA);
        String auth = bearer(equipo.dueno());

        mockMvc.perform(get("/api/citas").header("Authorization", auth)
                .param("salonId", ajeno.dueno().getSalon().getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(primera.getId().intValue(), tarde.getId().intValue())))
                .andExpect(jsonPath("$[0].length()").value(15));
        mockMvc.perform(get("/api/citas").header("Authorization", auth).param("fecha", hoy.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(manana.getId().intValue())));
    }

    @Test
    void estilistaSoloListaSusCitasAunqueEnvieElIdDeOtroEstilista() throws Exception {
        Cita propia = crearCita(equipo, equipo.estilista(), hoy.atTime(9, 0), EstadoCita.PENDIENTE);
        crearCita(equipo, equipo.otroEstilista(), hoy.atTime(10, 0), EstadoCita.CONFIRMADA);
        mockMvc.perform(get("/api/citas").header("Authorization", bearer(equipo.estilista()))
                .param("fecha", hoy.toString()).param("estilistaId", equipo.otroEstilista().getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(propia.getId().intValue())));
    }

    @Test
    void duenoConfirmaUnaPendienteYDevuelveLaFechaDeActualizacionPersistida() throws Exception {
        Cita cita = crearCita(equipo, equipo.estilista(), hoy.atTime(9, 0), EstadoCita.PENDIENTE);
        jdbc.update("update citas set fecha_creacion = ?, fecha_actualizacion = ? where id = ?",
                hoy.minusDays(1).atStartOfDay(), hoy.minusDays(1).atStartOfDay(), cita.getId());
        entityManager.clear();

        String respuesta = cambiar(equipo.dueno(), cita.getId(), "{\"estado\":\"CONFIRMADA\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.fechaCreacion").value(hoy.minusDays(1) + "T00:00:00"))
                .andReturn().getResponse().getContentAsString();
        LocalDateTime actualizacion = jdbc.queryForObject("select fecha_actualizacion from citas where id = ?",
                LocalDateTime.class, cita.getId());
        assertThat(actualizacion).isAfter(hoy.minusDays(1).atStartOfDay());
        String fechaEnRespuesta = JsonPath.read(respuesta, "$.fechaActualizacion");
        assertThat(LocalDateTime.parse(fechaEnRespuesta)).isEqualTo(actualizacion);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCita.class, names = {"COMPLETADA", "NO_ASISTIO"})
    void estilistaPuedeCerrarSoloSusCitasConfirmadas(EstadoCita destino) throws Exception {
        Cita cita = crearCita(equipo, equipo.estilista(), hoy.atTime(9, 0), EstadoCita.CONFIRMADA);
        cambiar(equipo.estilista(), cita.getId(), "{\"estado\":\"" + destino + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value(destino.name()));
    }

    @Test
    void aplica404AntesDelPermisoY403AntesDeValidarLaTransicion() throws Exception {
        Cita deCompanero = crearCita(equipo, equipo.otroEstilista(), hoy.atTime(9, 0), EstadoCita.CONFIRMADA);
        Cita propia = crearCita(equipo, equipo.estilista(), hoy.atTime(10, 0), EstadoCita.CONFIRMADA);
        Equipo ajeno = crearEquipo();
        Cita deOtroSalon = crearCita(ajeno, ajeno.estilista(), hoy.atTime(9, 0), EstadoCita.CONFIRMADA);
        cambiar(equipo.dueno(), deOtroSalon.getId(), "{\"estado\":\"COMPLETADA\"}")
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value(404));
        cambiar(equipo.estilista(), deCompanero.getId(), "{\"estado\":\"CANCELADA\"}")
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value(404));
        cambiar(equipo.estilista(), propia.getId(), "{\"estado\":\"PENDIENTE\"}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value(403));
        cambiar(equipo.dueno(), propia.getId(), "{\"estado\":\"PENDIENTE\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value(409));
    }

    @Test
    void rechazaFechaInvalidaYEstadosNoValidosCon400() throws Exception {
        Cita cita = crearCita(equipo, equipo.estilista(), hoy.atTime(9, 0), EstadoCita.CONFIRMADA);
        mockMvc.perform(get("/api/citas").header("Authorization", bearer(equipo.dueno())).param("fecha", "30/11/2026"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value(400));
        for (String json : new String[] {"{}", "{\"estado\":\"INVENTADO\"}", "{\"estado\":2}"}) {
            cambiar(equipo.dueno(), cita.getId(), json)
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value(400));
        }
    }

    private ResultActions cambiar(Usuario actor, Long citaId, String json) throws Exception {
        return mockMvc.perform(patch("/api/citas/{id}/estado", citaId).header("Authorization", bearer(actor))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String bearer(Usuario actor) {
        return "Bearer " + jwt.generarToken(actor.getEmail(), actor.getRol().name(), actor.getSalon().getId());
    }

    private Equipo crearEquipo() {
        Salon salon = new Salon();
        salon.setNombre("Salon de prueba");
        salon.setSlug("agenda-dia-" + UUID.randomUUID());
        salon.setPlan(Plan.GRATIS);
        salon.setFechaCreacion(hoy.atStartOfDay());
        entityManager.persist(salon);

        Servicio servicio = new Servicio();
        servicio.setSalon(salon);
        servicio.setNombre("Corte");
        servicio.setPrecio(35000);
        servicio.setDuracionMinutos(30);
        servicio.setActivo(true);
        entityManager.persist(servicio);

        Cliente cliente = new Cliente();
        cliente.setSalon(salon);
        cliente.setNombre("Sara");
        cliente.setTelefono("3001234567");
        entityManager.persist(cliente);

        return new Equipo(crearUsuario(salon, Rol.DUENO), crearUsuario(salon, Rol.ESTILISTA),
                crearUsuario(salon, Rol.ESTILISTA), servicio, cliente);
    }

    private Usuario crearUsuario(Salon salon, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setSalon(salon);
        usuario.setNombre(rol.name());
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuario.setEmail(UUID.randomUUID() + "@agenda.example");
        usuario.setPassword("valor-no-usado-para-autenticacion");
        entityManager.persist(usuario);
        return usuario;
    }

    private Cita crearCita(Equipo datos, Usuario estilista, LocalDateTime inicio, EstadoCita estado) {
        Cita cita = new Cita();
        cita.setSalon(estilista.getSalon());
        cita.setEstilista(estilista);
        cita.setServicio(datos.servicio());
        cita.setCliente(datos.cliente());
        cita.setInicio(inicio);
        cita.setFin(inicio.plusMinutes(30));
        cita.setEstado(estado);
        cita.setOrigen(estado == EstadoCita.PENDIENTE ? OrigenCita.EN_LINEA : OrigenCita.SALON);
        entityManager.persist(cita);
        return cita;
    }

    private record Equipo(Usuario dueno, Usuario estilista, Usuario otroEstilista, Servicio servicio, Cliente cliente) {
    }
}
