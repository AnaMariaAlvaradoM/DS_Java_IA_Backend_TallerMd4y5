package com.devsenior.sillon.cita;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.ia.FacturaIa;
import com.devsenior.sillon.ia.ProveedorIa;
import com.devsenior.sillon.ia.ProveedorNoDisponibleException;
import com.devsenior.sillon.ia.RespuestaIa;
import com.devsenior.sillon.ia.RespuestaIaInvalidaException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.seguridad.ServicioJwt;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
class RecordatorioIntegracionTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ServicioJwt jwt;
    @Autowired private Clock clock;
    @MockitoBean(name = "proveedorGemini") private ProveedorIa gemini;
    @MockitoBean(name = "proveedorClaude") private ProveedorIa claude;

    private final List<Long> salones = new ArrayList<>();
    private Datos propios;

    @BeforeEach
    void preparar() {
        propios = crearDatos();
    }

    @AfterEach
    void limpiar() {
        for (Long salonId : salones) {
            jdbc.update("delete from citas where salon_id = ?", salonId);
            jdbc.update("delete from usuarios where salon_id = ?", salonId);
            jdbc.update("delete from clientes where salon_id = ?", salonId);
            jdbc.update("delete from servicios where salon_id = ?", salonId);
            jdbc.update("delete from salones where id = ?", salonId);
        }
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCita.class, names = {"PENDIENTE", "CONFIRMADA"})
    void devuelveContratoExactoDatosVerificadosYFacturaSinEnviarMensajes(EstadoCita estado) throws Exception {
        jdbc.update("update citas set estado = ? where id = ?", estado.name(), propios.citaId());
        jdbc.update("update servicios set precio = 99999, duracion_minutos = 90 where salon_id = ?", propios.salonId());
        simular("{\"mensaje\":\"Hola Sara, le recordamos su cita a las 10:30. Confirme SÍ. Equipo del salón.\"}");
        recordar(propios, propios.citaId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$.citaId").value(propios.citaId()))
                .andExpect(jsonPath("$.telefonoCliente").value("3001234567"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("Sara")))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("10:30")))
                .andExpect(jsonPath("$.creditosRestantes").value(9))
                .andExpect(jsonPath("$.factura.length()").value(6))
                .andExpect(jsonPath("$.factura.proveedor").value("Gemini"))
                .andExpect(jsonPath("$.factura.modelo").value("modelo-simulado"))
                .andExpect(jsonPath("$.factura.tokensEntrada").value(30))
                .andExpect(jsonPath("$.factura.tokensSalida").value(20))
                .andExpect(jsonPath("$.factura.costoEstimadoUsd").value(0.00014))
                .andExpect(jsonPath("$.factura.milisegundos").value(7));
        var prompt = ArgumentCaptor.forClass(String.class);
        verify(gemini).responder(prompt.capture());
        assertThat(prompt.getValue()).contains("Salon recordatorio", "Sara", "Corte (45 minutos)", "Mateo",
                "martes 20 de octubre", "10:30 a. m.", "$35.000", "Calle 10 # 43-20, Medellín", "\"mensaje\"");
        assertThat(prompt.getValue()).doesNotContain("\u00a0", "\u202f", "99999", "90 minutos");
        verifyNoInteractions(claude);
        assertThat(creditos(propios)).isEqualTo(9);
        assertThat(jdbc.queryForObject("select estado from citas where id = ?", String.class, propios.citaId()))
                .isEqualTo(estado.name());
    }

    @Test
    void respuestaOficialSinNombreDelClienteDevuelve200SinModificarElMensaje() throws Exception {
        jdbc.update("update clientes set nombre = ?, telefono = ? where salon_id = ?",
                "Andrés Gómez", "3102223344", propios.salonId());
        jdbc.update("update usuarios set nombre = ? where salon_id = ? and rol = 'ESTILISTA'",
                "Sara Mejía", propios.salonId());
        String mensaje = "Hola. Le recordamos su cita. Servicio: Corte (45 minutos). "
                + "Estilista: Sara Mejía. Fecha: martes 20 de octubre. Hora: 10:30 a. m.. "
                + "Valor: $35.000. Dirección: Calle 10 # 43-20, Medellín. "
                + "¿Nos confirma su asistencia respondiendo SÍ?";
        simular("{\"mensaje\":\"" + mensaje + "\"}");

        recordar(propios, propios.citaId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$.citaId").value(propios.citaId()))
                .andExpect(jsonPath("$.telefonoCliente").value("3102223344"))
                .andExpect(jsonPath("$.mensaje").value(mensaje))
                .andExpect(jsonPath("$.creditosRestantes").value(9))
                .andExpect(jsonPath("$.factura.length()").value(6));

        assertThat(mensaje).doesNotContain("Andrés Gómez");
        assertThat(creditos(propios)).isEqualTo(9);
        verifyNoInteractions(claude);
    }

    @Test
    void sinCreditosDevuelve402SinLlamarAProveedores() throws Exception {
        jdbc.update("update salones set creditos_ia = 0 where id = ?", propios.salonId());
        recordar(propios, propios.citaId()).andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.codigo").value(402));
        verifyNoInteractions(gemini, claude);
        assertThat(creditos(propios)).isZero();
    }

    @Test
    void citaAjenaOInexistenteDevuelve404AntesDeVerificarCreditos() throws Exception {
        Datos ajenos = crearDatos();
        jdbc.update("update salones set creditos_ia = 0 where id = ?", propios.salonId());
        recordar(propios, ajenos.citaId()).andExpect(status().isNotFound());
        recordar(propios, Long.MAX_VALUE).andExpect(status().isNotFound());
        verifyNoInteractions(gemini, claude);
        assertThat(creditos(ajenos)).isEqualTo(10);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCita.class, names = {"COMPLETADA", "CANCELADA", "NO_ASISTIO"})
    void estadosFinalesDevuelven409SinConsumir(EstadoCita estado) throws Exception {
        jdbc.update("update citas set estado = ? where id = ?", estado.name(), propios.citaId());
        recordar(propios, propios.citaId()).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value(409));
        verifyNoInteractions(gemini, claude);
        assertThat(creditos(propios)).isEqualTo(10);
    }

    @ParameterizedTest
    @ValueSource(strings = {"no-json", "{}", "{\"mensaje\":\"\"}", "{\"mensaje\":123}",
            "{\"mensaje\":\"Hola Sara\"}", "{\"mensaje\":\"   \"}"})
    void respuestaRecibidaPeroInvalidaDevuelve502YConsumeSegunElPdf(String respuesta) throws Exception {
        simular(respuesta);
        recordar(propios, propios.citaId()).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$.codigo").value(502))
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(jsonPath("$.mensaje").isString())
                .andExpect(jsonPath("$.fecha").isString());
        assertThat(creditos(propios)).isEqualTo(9);
        verifyNoInteractions(claude);
    }

    @Test
    void mensajeMayorDe700CaracteresConsumePeroSeRechaza() throws Exception {
        simular("{\"mensaje\":\"Sara 10:30 " + "a".repeat(700) + "\"}");
        recordar(propios, propios.citaId()).andExpect(status().isBadGateway());
        assertThat(creditos(propios)).isEqualTo(9);
    }

    @Test
    void envoltorioHttpInvalidoTambienConsumeSinReintentarOtroProveedor() throws Exception {
        given(gemini.responder(anyString())).willThrow(new RespuestaIaInvalidaException());
        recordar(propios, propios.citaId()).andExpect(status().isBadGateway());
        assertThat(creditos(propios)).isEqualTo(9);
        verifyNoInteractions(claude);
    }

    @Test
    void ambosProveedoresNoDisponiblesDevuelven503SinConsumo() throws Exception {
        given(gemini.responder(anyString())).willThrow(new ProveedorNoDisponibleException(true));
        given(claude.responder(anyString())).willThrow(new ProveedorNoDisponibleException(true));
        recordar(propios, propios.citaId()).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$.codigo").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.mensaje").isString()).andExpect(jsonPath("$.fecha").isString());
        verify(claude).responder(anyString());
        assertThat(creditos(propios)).isEqualTo(10);
    }

    @Test
    void respaldoExitosoSoloConsumeUnCredito() throws Exception {
        given(gemini.responder(anyString())).willThrow(new ProveedorNoDisponibleException(false));
        given(claude.responder(anyString())).willAnswer(invocacion -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return respuesta("{\"mensaje\":\"Hola Sara, su cita es a las 10:30.\"}", "Claude");
        });
        recordar(propios, propios.citaId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.factura.proveedor").value("Claude"))
                .andExpect(jsonPath("$.creditosRestantes").value(9));
        assertThat(creditos(propios)).isEqualTo(9);
    }

    @Test
    void sinJwt401YRolesDistintosDeDueno403() throws Exception {
        mockMvc.perform(post("/api/citas/{id}/recordatorio", propios.citaId())).andExpect(status().isUnauthorized());
        for (String token : List.of(propios.tokenEstilista(), propios.tokenAdmin())) {
            mockMvc.perform(post("/api/citas/{id}/recordatorio", propios.citaId())
                    .header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        }
        verifyNoInteractions(gemini, claude);
        assertThat(creditos(propios)).isEqualTo(10);
    }

    @Test
    void unSoloCreditoConcurrenteNuncaSeDescuentaDosVecesEnMySql() throws Exception {
        jdbc.update("update salones set creditos_ia = 1 where id = ?", propios.salonId());
        var barrera = new CyclicBarrier(2);
        given(gemini.responder(anyString())).willAnswer(invocacion -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            barrera.await(10, TimeUnit.SECONDS);
            return respuesta("{\"mensaje\":\"Hola Sara, su cita es a las 10:30.\"}", "Gemini");
        });
        var executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> recordar(propios, propios.citaId()).andReturn().getResponse().getStatus());
            var b = executor.submit(() -> recordar(propios, propios.citaId()).andReturn().getResponse().getStatus());
            assertThat(List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 402);
            assertThat(creditos(propios)).isZero();
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(25, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void simular(String texto) {
        given(gemini.responder(anyString())).willAnswer(invocacion -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return respuesta(texto, "Gemini");
        });
    }

    private static RespuestaIa respuesta(String texto, String proveedor) {
        return new RespuestaIa(texto, new FacturaIa(proveedor, "modelo-simulado", 30, 20,
                new BigDecimal("0.00014"), 7));
    }

    private ResultActions recordar(Datos datos, Long citaId) throws Exception {
        return mockMvc.perform(post("/api/citas/{id}/recordatorio", citaId)
                .header("Authorization", "Bearer " + datos.tokenDueno()));
    }

    private int creditos(Datos datos) {
        return jdbc.queryForObject("select creditos_ia from salones where id = ?", Integer.class, datos.salonId());
    }

    private Datos crearDatos() {
        Datos datos = new TransactionTemplate(transactionManager).execute(estado -> {
            Salon salon = new Salon();
            salon.setNombre("Salon recordatorio");
            salon.setSlug("ia-" + UUID.randomUUID());
            salon.setCiudad("Medellín");
            salon.setDireccion("Calle 10 # 43-20");
            salon.setPlan(Plan.GRATIS);
            salon.setCreditosIa(10);
            salon.setFechaCreacion(LocalDateTime.now(clock));
            entityManager.persist(salon);
            Usuario estilista = usuario(salon, Rol.ESTILISTA);
            Usuario dueno = usuario(salon, Rol.DUENO);
            Usuario admin = usuario(salon, Rol.ADMIN);
            Cliente cliente = new Cliente();
            cliente.setSalon(salon);
            cliente.setNombre("Sara");
            cliente.setTelefono("3001234567");
            entityManager.persist(cliente);
            Servicio servicio = new Servicio();
            servicio.setSalon(salon);
            servicio.setNombre("Corte");
            servicio.setDuracionMinutos(45);
            servicio.setPrecio(35000);
            servicio.setActivo(true);
            entityManager.persist(servicio);
            Cita cita = new Cita();
            cita.setSalon(salon);
            cita.setCliente(cliente);
            cita.setEstilista(estilista);
            cita.setServicio(servicio);
            cita.setInicio(LocalDateTime.of(2026, 10, 20, 10, 30));
            cita.setFin(cita.getInicio().plusMinutes(45));
            cita.setEstado(EstadoCita.CONFIRMADA);
            cita.setOrigen(OrigenCita.SALON);
            entityManager.persist(cita);
            entityManager.flush();
            return new Datos(salon.getId(), cita.getId(), token(dueno), token(estilista), token(admin));
        });
        salones.add(datos.salonId());
        return datos;
    }

    private Usuario usuario(Salon salon, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setSalon(salon);
        usuario.setNombre(rol == Rol.ESTILISTA ? "Mateo" : "Dueno");
        usuario.setEmail(UUID.randomUUID() + "@recordatorio.example");
        usuario.setPassword("valor-no-usado-para-autenticacion");
        usuario.setRol(rol);
        usuario.setActivo(true);
        entityManager.persist(usuario);
        return usuario;
    }

    private String token(Usuario usuario) {
        return jwt.generarToken(usuario.getEmail(), usuario.getRol().name(), usuario.getSalon().getId());
    }

    private record Datos(Long salonId, Long citaId, String tokenDueno, String tokenEstilista, String tokenAdmin) {
    }
}
