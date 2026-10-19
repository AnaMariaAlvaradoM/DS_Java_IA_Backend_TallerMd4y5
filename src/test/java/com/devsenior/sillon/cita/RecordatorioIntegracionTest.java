package com.devsenior.sillon.cita;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.devsenior.sillon.auth.*;
import com.devsenior.sillon.cliente.ClienteRepository;
import com.devsenior.sillon.ia.AsistenteIA;
import com.devsenior.sillon.ia.IaNoDisponibleException;
import com.devsenior.sillon.ia.RespuestaIA;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.*;
import com.devsenior.sillon.usuario.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RecordatorioIntegracionTest.RelojPrueba.class)
class RecordatorioIntegracionTest {
    @TestConfiguration static class RelojPrueba {
        @Bean @Primary Clock relojRecordatorios() {
            return Clock.fixed(Instant.parse("2026-10-19T12:00:00Z"), ZoneId.of("America/Bogota"));
        }
    }

    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired ServicioService servicios;
    @Autowired EstilistaService estilistas;
    @Autowired CitaService citas;
    @Autowired CitaRepository citasRepo;
    @Autowired SalonRepository salones;
    @Autowired UsuarioRepository usuarios;
    @Autowired ServicioRepository serviciosRepo;
    @Autowired ClienteRepository clientes;
    @Autowired PlatformTransactionManager transacciones;
    @MockitoBean AsistenteIA ia;
    private final List<Long> salonesCreados = new ArrayList<>();
    private AuthResponse dueno;
    private EstilistaResponse sara;
    private CitaResponse cita;

    @BeforeEach void preparar() {
        dueno = registrar();
        var corte = servicios.crear(dueno.salonId(), new ServicioRequest("Corte clásico", null, 45, 35000L, true));
        sara = estilistas.crear(dueno.salonId(), new EstilistaRequest("Sara Mejía", email(), "secreta123", List.of(corte.id())));
        cita = citas.crear(dueno.salonId(), new CitaRequest("Andrés", "+57 (310) 222-3344", corte.id(), sara.id(),
                LocalDateTime.of(2026, 10, 20, 10, 30), null));
        when(ia.completar(anyString(), anyString())).thenAnswer(invocacion -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            String prompt = invocacion.getArgument(0);
            assertTrue(prompt.contains("Servicio: Corte clásico (45 minutos)"));
            assertTrue(prompt.contains("Fecha: martes 20 de octubre"));
            assertTrue(prompt.contains("Hora: 10:30 a. m."));
            assertTrue(prompt.contains("Valor: $35.000"));
            assertTrue(prompt.contains("Dirección: Calle 10 # 43-20, Medellín"));
            return respuesta("{\"mensaje\":\"Hola Andrés, Sara Mejía le espera a las 10:30 a. m.\"}");
        });
    }

    @AfterEach void limpiar() {
        new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
            if (cita != null) { citasRepo.deleteById(cita.id()); }
            for (Long salonId : salonesCreados) {
                clientes.deleteAll(clientes.findAllBySalonId(salonId));
                usuarios.deleteAll(usuarios.findAllBySalonIdAndRol(salonId, Rol.ESTILISTA));
                usuarios.deleteAll(usuarios.findAllBySalonIdAndRol(salonId, Rol.DUENO));
                usuarios.flush();
                serviciosRepo.deleteAll(serviciosRepo.findAllBySalonId(salonId));
                serviciosRepo.flush(); salones.deleteById(salonId);
            }
        });
    }

    @Test void duenoRecibeContratoFacturaYQuedanNueveSinCambiarCita() throws Exception {
        mvc.perform(post(ruta()).header("Authorization", bearer(dueno)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$.citaId").value(cita.id()))
                .andExpect(jsonPath("$.telefonoCliente").value("+573102223344"))
                .andExpect(jsonPath("$.mensaje").value("Hola Andrés, Sara Mejía le espera a las 10:30 a. m."))
                .andExpect(jsonPath("$.creditosRestantes").value(9))
                .andExpect(jsonPath("$.factura.length()").value(6))
                .andExpect(jsonPath("$.factura.proveedor").value("openai"));
        assertEquals(9, saldo());
        assertEquals(EstadoCita.CONFIRMADA, citasRepo.findById(cita.id()).orElseThrow().getEstado());
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "no es JSON", "{\"mensaje\":\" \"}"})
    void jsonInvalidoEs502YConsumoPersiste(String texto) throws Exception {
        doReturn(respuesta(texto)).when(ia).completar(anyString(), anyString());
        mvc.perform(post(ruta()).header("Authorization", bearer(dueno)))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.codigo").value(502));
        assertEquals(9, saldo());
    }

    @Test void sinCreditosEs402YNoLlamaIa() throws Exception {
        ponerSaldo(0);
        mvc.perform(post(ruta()).header("Authorization", bearer(dueno)))
                .andExpect(status().isPaymentRequired());
        verifyNoInteractions(ia); assertEquals(0, saldo());
    }

    @Test void completadaEs409AntesDelSaldoYNoLlamaIa() throws Exception {
        new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
            Cita guardada = citasRepo.findById(cita.id()).orElseThrow(); guardada.setEstado(EstadoCita.COMPLETADA);
        });
        ponerSaldo(0);
        mvc.perform(post(ruta()).header("Authorization", bearer(dueno)))
                .andExpect(status().isConflict());
        verifyNoInteractions(ia); assertEquals(0, saldo());
    }

    @Test void citaAjenaEs404YSinAutenticacionEs401() throws Exception {
        AuthResponse otro = registrar();
        mvc.perform(post(ruta()).header("Authorization", bearer(otro))).andExpect(status().isNotFound());
        mvc.perform(post("/api/citas/-1/recordatorio").header("Authorization", bearer(dueno))).andExpect(status().isNotFound());
        mvc.perform(post(ruta())).andExpect(status().isUnauthorized());
        verifyNoInteractions(ia); assertEquals(10, saldo());
    }

    @Test void estilistaNoPuedeGenerarRecordatorio() throws Exception {
        AuthResponse sesion = auth.login(new LoginRequest(sara.email(), "secreta123"));
        mvc.perform(post(ruta()).header("Authorization", bearer(sesion))).andExpect(status().isForbidden());
        verifyNoInteractions(ia); assertEquals(10, saldo());
    }

    @Test void proveedoresNoDisponiblesEs503YSinConsumo() throws Exception {
        doThrow(new IaNoDisponibleException()).when(ia).completar(anyString(), anyString());
        mvc.perform(post(ruta()).header("Authorization", bearer(dueno)))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.codigo").value(503));
        assertEquals(10, saldo());
    }

    private AuthResponse registrar() {
        AuthResponse creada = auth.registrar(new RegistroRequest("Recordatorio " + UUID.randomUUID(), "Medellín", "Calle 10 # 43-20",
                "6045551234", "Camilo", email(), "secreta123"));
        salonesCreados.add(creada.salonId()); return creada;
    }
    private String email() { return "recordatorio." + UUID.randomUUID() + "@ejemplo.test"; }
    private String ruta() { return "/api/citas/" + cita.id() + "/recordatorio"; }
    private String bearer(AuthResponse sesion) { return "Bearer " + sesion.token(); }
    private int saldo() { return salones.consultarCreditos(dueno.salonId()).orElseThrow(); }
    private void ponerSaldo(int cantidad) {
        new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
            var salon = salones.findById(dueno.salonId()).orElseThrow(); salon.setCreditosIa(cantidad);
        });
    }
    private RespuestaIA respuesta(String texto) {
        return new RespuestaIA(texto, "openai", "simulado", 20, 10, new BigDecimal("0.000031"), 15);
    }
}
