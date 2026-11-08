package com.devsenior.sillon.cita;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CitaIntegracionTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired Clock clock;

    @Test
    void agendarValidarAislarYActualizarEstado() throws Exception {
        JsonNode duenoA = registrar("Agenda A");
        JsonNode duenoB = registrar("Agenda B");
        String tokenA = duenoA.get("token").asString();
        String tokenB = duenoB.get("token").asString();
        long corte = crearServicio(tokenA, "Corte", 45, 35000);
        long barba = crearServicio(tokenA, "Barba", 30, 20000);
        long ajeno = crearServicio(tokenB, "Servicio B", 45, 35000);
        String emailMateo = "mateo-" + UUID.randomUUID() + "@ejemplo.co";
        String emailSara = "sara-" + UUID.randomUUID() + "@ejemplo.co";
        long mateo = crearEstilista(tokenA, emailMateo, corte + "," + barba);
        long sara = crearEstilista(tokenA, emailSara, String.valueOf(corte));
        String tokenMateo = login(emailMateo);
        LocalDate dia = diaLaboral();

        JsonNode primera = json.readTree(agendar(tokenA, corte, mateo, dia.atTime(10, 0), "Valentina", "300 111 2233")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.origen").value("SALON"))
                .andExpect(jsonPath("$.precio").value(35000))
                .andExpect(jsonPath("$.clienteTelefono").value("3001112233"))
                .andExpect(jsonPath("$.fin").value(dia + "T10:45:00"))
                .andReturn().getResponse().getContentAsString());
        long citaMateo = primera.get("id").asLong();

        mvc.perform(put("/api/servicios/{id}", corte).header("Authorization", bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Corte\",\"duracionMinutos\":45,\"precio\":40000}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/citas").header("Authorization", bearer(tokenA)).param("fecha", dia.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].precio").value(35000));

        agendar(tokenA, corte, mateo, dia.atTime(10, 30), "Cruce", "3001112234")
                .andExpect(status().isConflict());
        agendar(tokenA, corte, mateo, dia.atTime(10, 45), "Consecutiva", "3001112235")
                .andExpect(status().isCreated());
        JsonNode citaSaraJson = json.readTree(agendar(tokenA, corte, sara, dia.atTime(10, 30), "Andrés", "310 222 3344")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long citaSara = citaSaraJson.get("id").asLong();

        agendar(tokenA, barba, sara, dia.atTime(12, 0), "Barba", "3001112236")
                .andExpect(status().isConflict());
        agendar(tokenA, ajeno, mateo, dia.atTime(12, 0), "Ajeno", "3001112237")
                .andExpect(status().isNotFound());
        agendar(tokenA, corte, mateo, dia.atTime(18, 45), "Tarde", "3001112238")
                .andExpect(status().isConflict());
        agendar(tokenA, corte, mateo, LocalDate.now(clock).minusDays(1).atTime(10, 0), "Pasado", "3001112239")
                .andExpect(status().isConflict());
        LocalDate domingo = dia;
        while (domingo.getDayOfWeek() != DayOfWeek.SUNDAY) { domingo = domingo.plusDays(1); }
        agendar(tokenA, corte, mateo, domingo.atTime(10, 0), "Domingo", "3001112240")
                .andExpect(status().isConflict());

        mvc.perform(get("/api/citas").header("Authorization", bearer(tokenA)).param("fecha", dia.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(get("/api/citas").header("Authorization", bearer(tokenMateo)).param("fecha", dia.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/citas").header("Authorization", bearer(tokenB)).param("fecha", dia.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        agendar(tokenMateo, corte, mateo, dia.atTime(13, 0), "Prohibido", "3001112241")
                .andExpect(status().isForbidden());

        cambiar(tokenB, citaMateo, "CANCELADA").andExpect(status().isNotFound());
        cambiar(tokenMateo, citaSara, "COMPLETADA").andExpect(status().isNotFound());
        cambiar(tokenMateo, citaMateo, "CANCELADA").andExpect(status().isForbidden());
        JsonNode actualizada = json.readTree(cambiar(tokenMateo, citaMateo, "COMPLETADA")
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("COMPLETADA"))
                .andReturn().getResponse().getContentAsString());
        assertTrue(LocalDateTime.parse(actualizada.get("fechaActualizacion").asString())
                .isAfter(LocalDateTime.parse(primera.get("fechaCreacion").asString())));
        cambiar(tokenA, citaMateo, "PENDIENTE").andExpect(status().isConflict());
        cambiar(tokenA, citaSara, "CANCELADA").andExpect(status().isOk());
        agendar(tokenA, corte, mateo, dia.atTime(10, 0), "Nueva cita", "3001112242")
                .andExpect(status().isCreated());
    }

    private JsonNode registrar(String nombre) throws Exception {
        String email = "dueno-" + UUID.randomUUID() + "@ejemplo.co";
        String cuerpo = """
                {"salonNombre":"%s","ciudad":"Bogotá","direccion":"Calle 1","telefono":"1234567",
                 "nombre":"Dueño","email":"%s","password":"secreta123"}
                """.formatted(nombre, email);
        MvcResult resultado = mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(resultado.getResponse().getContentAsString());
    }

    private long crearServicio(String token, String nombre, int duracion, int precio) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/servicios").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"" + nombre + "\",\"duracionMinutos\":" + duracion + ",\"precio\":" + precio + "}"))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private long crearEstilista(String token, String email, String ids) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/estilistas").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"" + (email.contains("mateo") ? "Mateo" : "Sara")
                        + "\",\"email\":\"" + email + "\",\"password\":\"estilista123\",\"servicioIds\":[" + ids + "]}"))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private String login(String email) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"estilista123\"}"))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(resultado.getResponse().getContentAsString()).get("token").asString();
    }

    private org.springframework.test.web.servlet.ResultActions agendar(String token, long servicio, long estilista,
            LocalDateTime inicio, String cliente, String telefono) throws Exception {
        String cuerpo = """
                {"clienteNombre":"%s","clienteTelefono":"%s","servicioId":%d,
                 "estilistaId":%d,"inicio":"%s"}
                """.formatted(cliente, telefono, servicio, estilista, inicio);
        return mvc.perform(post("/api/citas").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private org.springframework.test.web.servlet.ResultActions cambiar(String token, long citaId, String estado)
            throws Exception {
        return mvc.perform(patch("/api/citas/{id}/estado", citaId).header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"" + estado + "\"}"));
    }

    private LocalDate diaLaboral() {
        LocalDate fecha = LocalDate.now(clock).plusDays(2);
        while (fecha.getDayOfWeek() == DayOfWeek.SUNDAY) { fecha = fecha.plusDays(1); }
        return fecha;
    }

    private String bearer(String token) { return "Bearer " + token; }
}
