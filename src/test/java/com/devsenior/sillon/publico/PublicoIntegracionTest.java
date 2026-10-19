package com.devsenior.sillon.publico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.devsenior.sillon.auth.AuthResponse;
import com.devsenior.sillon.auth.AuthService;
import com.devsenior.sillon.auth.RegistroRequest;
import com.devsenior.sillon.cita.CitaRequest;
import com.devsenior.sillon.cita.CitaResponse;
import com.devsenior.sillon.cita.EstadoCita;
import com.devsenior.sillon.cita.OrigenCita;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.servicio.ServicioRequest;
import com.devsenior.sillon.servicio.ServicioResponse;
import com.devsenior.sillon.servicio.ServicioService;
import com.devsenior.sillon.usuario.EstilistaRequest;
import com.devsenior.sillon.usuario.EstilistaResponse;
import com.devsenior.sillon.usuario.EstilistaService;
import com.devsenior.sillon.usuario.UsuarioRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
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
class PublicoIntegracionTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private AuthService auth;
    @Autowired private ServicioService servicios;
    @Autowired private EstilistaService estilistas;
    @Autowired private ServicioRepository serviciosRepo;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private Clock reloj;
    private AuthResponse dueno;
    private ServicioResponse corte;
    private ServicioResponse barba;
    private EstilistaResponse mateo;
    private EstilistaResponse sara;
    private LocalDate fecha;
    private String ruta;

    @BeforeEach void preparar() {
        dueno = dueno();
        corte = servicios.crear(dueno.salonId(), new ServicioRequest("Corte clásico", null, 45, 35000L, true));
        barba = servicios.crear(dueno.salonId(), new ServicioRequest("Arreglo de barba", null, 30, 20000L, true));
        mateo = estilistas.crear(dueno.salonId(), new EstilistaRequest("Mateo Gil", email(), "secreta123", List.of(corte.id(), barba.id())));
        sara = estilistas.crear(dueno.salonId(), new EstilistaRequest("Sara Mejía", email(), "secreta123", List.of(corte.id())));
        fecha = LocalDate.now(reloj).plusDays(2);
        while (fecha.getDayOfWeek() == DayOfWeek.SUNDAY) { fecha = fecha.plusDays(1); }
        ruta = "/api/publico/salones/" + dueno.salonSlug();
    }

    @Test void catalogoPublicoNoExigeTokenNiRevelaCorreos() throws Exception {
        var resultado = mvc.perform(get(ruta)).andExpect(status().isOk())
                .andExpect(jsonPath("$.servicios.length()").value(2))
                .andExpect(jsonPath("$.estilistas.length()").value(2))
                .andExpect(jsonPath("$.horario").value("Lunes a sábado (domingos cerrado), de 8:00 a. m. a 7:00 p. m."))
                .andExpect(jsonPath("$.estilistas[0].email").doesNotExist())
                .andExpect(jsonPath("$.estilistas[0].password").doesNotExist()).andReturn();
        assertFalse(resultado.getResponse().getContentAsString().contains("@"));
    }

    @Test void excluyeServiciosYEstilistasInactivos() throws Exception {
        var servicio = serviciosRepo.findById(corte.id()).orElseThrow();
        servicio.setActivo(false); serviciosRepo.saveAndFlush(servicio);
        var estilista = usuarios.findById(sara.id()).orElseThrow();
        estilista.setActivo(false); usuarios.saveAndFlush(estilista);
        mvc.perform(get(ruta)).andExpect(status().isOk())
                .andExpect(jsonPath("$.servicios.length()").value(1))
                .andExpect(jsonPath("$.servicios[0].id").value(barba.id()))
                .andExpect(jsonPath("$.estilistas.length()").value(1))
                .andExpect(jsonPath("$.estilistas[0].servicioIds.length()").value(1))
                .andExpect(jsonPath("$.estilistas[0].servicioIds[0]").value(barba.id()));
    }

    @Test void reservaActualizaDisponibilidadYBloqueaCitaInterna() throws Exception {
        mvc.perform(disponibilidad()).andExpect(status().isOk())
                .andExpect(jsonPath("$.servicio").value("Corte clásico"))
                .andExpect(jsonPath("$.estilista").value("Mateo Gil"))
                .andExpect(jsonPath("$.duracionMinutos").value(45))
                .andExpect(jsonPath("$.horariosLibres", org.hamcrest.Matchers.hasItem("10:00:00")));
        CitaResponse reserva = reservar();
        assertEquals(EstadoCita.PENDIENTE, reserva.estado());
        assertEquals(OrigenCita.EN_LINEA, reserva.origen());
        assertEquals(fecha.atTime(10, 45), reserva.fin());
        assertEquals(35000, reserva.precio());
        mvc.perform(disponibilidad()).andExpect(status().isOk())
                .andExpect(jsonPath("$.horariosLibres", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("09:30:00"))))
                .andExpect(jsonPath("$.horariosLibres", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("10:00:00"))))
                .andExpect(jsonPath("$.horariosLibres", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("10:30:00"))))
                .andExpect(jsonPath("$.horariosLibres", org.hamcrest.Matchers.hasItem("11:00:00")));
        mvc.perform(post("/api/citas").header("Authorization", "Bearer " + dueno.token())
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(
                                new CitaRequest("Andrés", "3102223344", corte.id(), mateo.id(), fecha.atTime(10, 30), null))))
                .andExpect(status().isConflict());
    }

    @Test void cancelarReservaLiberaElHorario() throws Exception {
        CitaResponse reserva = reservar();
        mvc.perform(patch("/api/citas/{id}/estado", reserva.id()).header("Authorization", "Bearer " + dueno.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CANCELADA\"}"))
                .andExpect(status().isOk());
        mvc.perform(disponibilidad()).andExpect(status().isOk())
                .andExpect(jsonPath("$.horariosLibres", org.hamcrest.Matchers.hasItem("10:00:00")));
    }

    @Test void faltanParametrosEs400() throws Exception {
        mvc.perform(get(ruta + "/disponibilidad").param("servicioId", corte.id().toString())
                        .param("estilistaId", mateo.id().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("fecha")));
    }

    @Test void fechaInvalidaEs400() throws Exception {
        mvc.perform(get(ruta + "/disponibilidad").param("servicioId", corte.id().toString())
                        .param("estilistaId", mateo.id().toString()).param("fecha", "invalida"))
                .andExpect(status().isBadRequest());
    }

    @Test void salonInexistenteEs404SinToken() throws Exception {
        mvc.perform(get("/api/publico/salones/no-existe-" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest @ValueSource(strings = {"servicio", "estilista"})
    void idsDeOtroSalonSon404(String tipo) throws Exception {
        AuthResponse otro = dueno();
        ServicioResponse ajeno = servicios.crear(otro.salonId(), new ServicioRequest("Corte ajeno", null, 45, 35000L, true));
        EstilistaResponse ajenoEstilista = estilistas.crear(otro.salonId(),
                new EstilistaRequest("Otro", email(), "secreta123", List.of(ajeno.id())));
        Long servicioId = tipo.equals("servicio") ? ajeno.id() : corte.id();
        Long estilistaId = tipo.equals("estilista") ? ajenoEstilista.id() : mateo.id();
        mvc.perform(get(ruta + "/disponibilidad").param("servicioId", servicioId.toString())
                        .param("estilistaId", estilistaId.toString()).param("fecha", fecha.toString()))
                .andExpect(status().isNotFound());
    }

    @Test void estilistaQueNoOfreceServicioEs409() throws Exception {
        mvc.perform(get(ruta + "/disponibilidad").param("servicioId", barba.id().toString())
                        .param("estilistaId", sara.id().toString()).param("fecha", fecha.toString()))
                .andExpect(status().isConflict());
    }

    @Test void domingoCerradoNoTieneHorarios() throws Exception {
        LocalDate domingo = fecha.with(java.time.temporal.TemporalAdjusters.next(DayOfWeek.SUNDAY));
        mvc.perform(get(ruta + "/disponibilidad").param("servicioId", corte.id().toString())
                        .param("estilistaId", mateo.id().toString()).param("fecha", domingo.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.horariosLibres.length()").value(0));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder disponibilidad() {
        return get(ruta + "/disponibilidad").param("servicioId", corte.id().toString())
                .param("estilistaId", mateo.id().toString()).param("fecha", fecha.toString());
    }
    private CitaResponse reservar() throws Exception {
        var resultado = mvc.perform(post(ruta + "/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new CitaRequest("Valentina Ruiz", "3001112233",
                                corte.id(), mateo.id(), fecha.atTime(10, 0), null))))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readValue(resultado.getResponse().getContentAsString(), CitaResponse.class);
    }
    private AuthResponse dueno() {
        return auth.registrar(new RegistroRequest("Salón " + UUID.randomUUID(), "Medellín", "Calle 10 # 43-20",
                "6045551234", "Camilo", email(), "secreta123"));
    }
    private String email() { return "persona." + UUID.randomUUID() + "@ejemplo.test"; }
}
