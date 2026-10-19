package com.devsenior.sillon.cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.devsenior.sillon.auth.AuthResponse;
import com.devsenior.sillon.auth.AuthService;
import com.devsenior.sillon.auth.LoginRequest;
import com.devsenior.sillon.auth.RegistroRequest;
import com.devsenior.sillon.servicio.ServicioRequest;
import com.devsenior.sillon.servicio.ServicioResponse;
import com.devsenior.sillon.servicio.ServicioService;
import com.devsenior.sillon.usuario.EstilistaRequest;
import com.devsenior.sillon.usuario.EstilistaResponse;
import com.devsenior.sillon.usuario.EstilistaService;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class AgendaIntegracionTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private AuthService auth;
    @Autowired private ServicioService servicios;
    @Autowired private EstilistaService estilistas;
    @Autowired private CitaService citas;
    @Autowired private Clock reloj;
    private AuthResponse dueno;
    private ServicioResponse corte;
    private ServicioResponse barba;
    private EstilistaResponse sara;
    private EstilistaResponse mateo;
    private LocalDate dia;

    @BeforeEach void preparar() {
        dueno = auth.registrar(new RegistroRequest("Salón " + UUID.randomUUID(), "Bogotá", "Calle 1", "3001112233",
                "Dueño", email(), "secreta123"));
        corte = servicios.crear(dueno.salonId(), new ServicioRequest("Corte", null, 45, 35000L, true));
        barba = servicios.crear(dueno.salonId(), new ServicioRequest("Barba", null, 30, 20000L, true));
        sara = estilistas.crear(dueno.salonId(), new EstilistaRequest("Sara", email(), "secreta123", List.of(corte.id())));
        mateo = estilistas.crear(dueno.salonId(), new EstilistaRequest("Mateo", email(), "secreta123", List.of(corte.id(), barba.id())));
        dia = LocalDate.now(reloj).plusDays(2);
        while (dia.getDayOfWeek() == DayOfWeek.SUNDAY) { dia = dia.plusDays(1); }
    }

    @Test void creaCitaConContratoCompletoYPrecioHistorico() throws Exception {
        var resultado = mvc.perform(post("/api/citas").header("Authorization", bearer(dueno))
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request(sara, corte, dia.atTime(10, 30)))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.length()").value(15))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.origen").value("SALON"))
                .andExpect(jsonPath("$.clienteTelefono").value("+573102223344"))
                .andExpect(jsonPath("$.precio").value(35000)).andReturn();
        CitaResponse creada = mapper.readValue(resultado.getResponse().getContentAsString(), CitaResponse.class);
        assertEquals(dia.atTime(11, 15), creada.fin());
        servicios.editar(dueno.salonId(), corte.id(), new ServicioRequest("Corte", null, 45, 50000L, true));
        mvc.perform(get("/api/citas").param("fecha", dia.toString()).header("Authorization", bearer(dueno)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].precio").value(35000));
    }

    @ParameterizedTest @ValueSource(strings = {"pasado", "domingo", "cierre", "apertura", "servicio"})
    void rechazaLasReglasDeHorarioYServicio(String caso) throws Exception {
        LocalDateTime inicio = dia.atTime(10, 30);
        ServicioResponse servicio = corte;
        switch (caso) {
            case "pasado" -> inicio = LocalDateTime.now(reloj).minusDays(1);
            case "domingo" -> inicio = dia.with(java.time.temporal.TemporalAdjusters.next(DayOfWeek.SUNDAY)).atTime(10, 0);
            case "cierre" -> inicio = dia.atTime(18, 45);
            case "apertura" -> inicio = dia.atTime(7, 45);
            case "servicio" -> servicio = barba;
        }
        mvc.perform(post("/api/citas").header("Authorization", bearer(dueno)).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request(sara, servicio, inicio))))
                .andExpect(status().isConflict());
    }

    @Test void cruceEs409() throws Exception {
        citas.crear(dueno.salonId(), request(mateo, corte, dia.atTime(10, 0)));
        mvc.perform(post("/api/citas").header("Authorization", bearer(dueno)).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request(mateo, corte, dia.atTime(10, 30)))))
                .andExpect(status().isConflict());
    }

    @Test void permiteCitasContiguasYDistintosEstilistas() {
        citas.crear(dueno.salonId(), request(mateo, corte, dia.atTime(10, 0)));
        assertEquals(dia.atTime(10, 45), citas.crear(dueno.salonId(), request(mateo, corte, dia.atTime(10, 45))).inicio());
        assertEquals(EstadoCita.CONFIRMADA, citas.crear(dueno.salonId(), request(sara, corte, dia.atTime(10, 0))).estado());
    }

    @Test void confirmarReservaActualizaFechaEnLaRespuesta() throws Exception {
        CitaResponse pendiente = citas.reservar(dueno.salonId(), request(mateo, corte, dia.atTime(10, 0)));
        assertEquals(EstadoCita.PENDIENTE, pendiente.estado());
        assertEquals(OrigenCita.EN_LINEA, pendiente.origen());
        var resultado = mvc.perform(patch("/api/citas/{id}/estado", pendiente.id()).header("Authorization", bearer(dueno))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CONFIRMADA\"}"))
                .andExpect(status().isOk()).andReturn();
        CitaResponse confirmada = mapper.readValue(resultado.getResponse().getContentAsString(), CitaResponse.class);
        assertTrue(confirmada.fechaActualizacion().isAfter(confirmada.fechaCreacion()));
        assertEquals(EstadoCita.CONFIRMADA, confirmada.estado());
    }

    @Test void estilistaSoloVeSusCitasYCambiaLasSuyas() throws Exception {
        CitaResponse deMateo = citas.crear(dueno.salonId(), request(mateo, corte, dia.atTime(10, 0)));
        CitaResponse deSara = citas.crear(dueno.salonId(), request(sara, corte, dia.atTime(10, 30)));
        AuthResponse sesion = auth.login(new LoginRequest(mateo.email(), "secreta123"));
        mvc.perform(get("/api/citas").param("fecha", dia.toString()).header("Authorization", bearer(sesion)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(deMateo.id()));
        mvc.perform(patch("/api/citas/{id}/estado", deSara.id()).header("Authorization", bearer(sesion))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"COMPLETADA\"}"))
                .andExpect(status().isNotFound());
    }

    @Test void estilistaNoPuedeCancelarSuCita() throws Exception {
        CitaResponse cita = citas.crear(dueno.salonId(), request(mateo, corte, dia.atTime(10, 0)));
        AuthResponse sesion = auth.login(new LoginRequest(mateo.email(), "secreta123"));
        mvc.perform(patch("/api/citas/{id}/estado", cita.id()).header("Authorization", bearer(sesion))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CANCELADA\"}"))
                .andExpect(status().isForbidden());
    }

    @Test void estilistaCompletaSuCitaYNoPuedeVolverAPendiente() throws Exception {
        CitaResponse cita = citas.crear(dueno.salonId(), request(mateo, corte, dia.atTime(10, 0)));
        AuthResponse sesion = auth.login(new LoginRequest(mateo.email(), "secreta123"));
        mvc.perform(patch("/api/citas/{id}/estado", cita.id()).header("Authorization", bearer(sesion))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"COMPLETADA\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("COMPLETADA"));
        mvc.perform(patch("/api/citas/{id}/estado", cita.id()).header("Authorization", bearer(dueno))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"PENDIENTE\"}"))
                .andExpect(status().isConflict());
    }

    @Test void otroSalonNoVeNiCambiaLaCita() throws Exception {
        CitaResponse cita = citas.crear(dueno.salonId(), request(mateo, corte, dia.atTime(10, 0)));
        AuthResponse otro = auth.registrar(new RegistroRequest("Otro " + UUID.randomUUID(), "Bogotá", "Calle 2",
                "3001112233", "Otra dueña", email(), "secreta123"));
        mvc.perform(get("/api/citas").param("fecha", dia.toString()).header("Authorization", bearer(otro)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(patch("/api/citas/{id}/estado", cita.id()).header("Authorization", bearer(otro))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CANCELADA\"}"))
                .andExpect(status().isNotFound());
    }

    @Test void telefonoSinDigitosEs400() throws Exception {
        CitaRequest request = new CitaRequest("Andrés", "abcdefghi", corte.id(), sara.id(), dia.atTime(10, 30), null);
        mvc.perform(post("/api/citas").header("Authorization", bearer(dueno)).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))).andExpect(status().isBadRequest());
    }

    @Test void duenoVeTodasLasCitasOrdenadas() throws Exception {
        citas.crear(dueno.salonId(), request(sara, corte, dia.atTime(11, 0)));
        citas.crear(dueno.salonId(), request(mateo, corte, dia.atTime(10, 0)));
        mvc.perform(get("/api/citas").param("fecha", dia.toString()).header("Authorization", bearer(dueno)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].estilistaNombre").value("Mateo"));
    }

    private CitaRequest request(EstilistaResponse estilista, ServicioResponse servicio, LocalDateTime inicio) {
        return new CitaRequest("Andrés", "+57 (310) 222-3344", servicio.id(), estilista.id(), inicio, null);
    }
    private String email() { return "persona." + UUID.randomUUID() + "@ejemplo.test"; }
    private String bearer(AuthResponse sesion) { return "Bearer " + sesion.token(); }
}
