package com.devsenior.sillon.publico;

import com.devsenior.sillon.usuario.UsuarioRepositorio;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicoIntegracionTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired Clock clock;
    @Autowired UsuarioRepositorio usuarios;

    @Test
    void flujoPublicoCompletoYAislamiento() throws Exception {
        JsonNode duenoA = registrar("La Navaja");
        JsonNode duenoB = registrar("Aurora");
        String tokenA = duenoA.get("token").asString();
        String tokenB = duenoB.get("token").asString();
        String slugA = duenoA.get("salonSlug").asString();
        String slugB = duenoB.get("salonSlug").asString();
        long corte = crearServicio(tokenA, "Corte clásico", 45, 35000);
        long barba = crearServicio(tokenA, "Barba", 30, 20000);
        long servicioB = crearServicio(tokenB, "Peinado", 60, 40000);
        String emailMateo = "mateo-" + UUID.randomUUID() + "@ejemplo.co";
        long mateo = crearEstilista(tokenA, "Mateo", emailMateo, corte + "," + barba);
        long sara = crearEstilista(tokenA, "Sara", "sara-" + UUID.randomUUID() + "@ejemplo.co",
                String.valueOf(corte));
        crearEstilista(tokenB, "Diana", "diana-" + UUID.randomUUID() + "@ejemplo.co", String.valueOf(servicioB));
        usuarios.findById(sara).orElseThrow().setActivo(false);
        mvc.perform(put("/api/servicios/{id}", barba).header("Authorization", bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Barba\",\"duracionMinutos\":30,\"precio\":20000,\"activo\":false}"))
                .andExpect(status().isOk());

        MvcResult catalogo = mvc.perform(get("/api/publico/salones/{slug}", slugA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("La Navaja"))
                .andExpect(jsonPath("$.horario").isString())
                .andExpect(jsonPath("$.servicios", hasSize(1)))
                .andExpect(jsonPath("$.estilistas", hasSize(1)))
                .andExpect(jsonPath("$.estilistas[0].servicioIds", hasSize(1)))
                .andReturn();
        String catalogoTexto = catalogo.getResponse().getContentAsString();
        assertFalse(catalogoTexto.contains("@"));
        assertFalse(catalogoTexto.contains("password"));
        assertFalse(catalogoTexto.contains("creditosIa"));
        assertFalse(catalogoTexto.contains("Aurora"));
        mvc.perform(get("/api/publico/salones/no-existe"))
                .andExpect(status().isNotFound());

        LocalDate dia = diaLaboral();
        JsonNode antes = json.readTree(disponibilidad(slugA, corte, mateo, dia)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicio").value("Corte clásico"))
                .andExpect(jsonPath("$.estilista").value("Mateo"))
                .andExpect(jsonPath("$.duracionMinutos").value(45))
                .andReturn().getResponse().getContentAsString());
        assertTrue(antes.get("horariosLibres").toString().contains("10:00:00"));

        JsonNode reserva = json.readTree(reservar(slugA, corte, mateo, dia, "10:00", "Valentina", "300 111 2233")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.origen").value("EN_LINEA"))
                .andExpect(jsonPath("$.fin").value(dia + "T10:45:00"))
                .andExpect(jsonPath("$.precio").value(35000))
                .andReturn().getResponse().getContentAsString());
        long citaId = reserva.get("id").asLong();
        JsonNode despues = json.readTree(disponibilidad(slugA, corte, mateo, dia)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String horas = despues.get("horariosLibres").toString();
        assertFalse(horas.contains("09:30:00"));
        assertFalse(horas.contains("10:00:00"));
        assertFalse(horas.contains("10:30:00"));
        assertTrue(horas.contains("11:00:00"));
        reservar(slugA, corte, mateo, dia, "10:30", "Cruce", "300 111 2234")
                .andExpect(status().isConflict());
        reservar(slugA, corte, mateo, dia, "18:45", "Tarde", "300 111 2235")
                .andExpect(status().isConflict());
        reservar(slugA, corte, mateo, dia, "11:00", "Andrés", "310 222 3344")
                .andExpect(status().isCreated());

        mvc.perform(patch("/api/citas/{id}/estado", citaId).header("Authorization", bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CONFIRMADA\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CONFIRMADA"));
        mvc.perform(patch("/api/citas/{id}/estado", citaId).header("Authorization", bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"PENDIENTE\"}"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/citas/{id}/estado", citaId).header("Authorization", bearer(tokenB))
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CANCELADA\"}"))
                .andExpect(status().isNotFound());
        String tokenMateo = login(emailMateo);
        mvc.perform(get("/api/citas").header("Authorization", bearer(tokenMateo)).param("fecha", dia.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(patch("/api/citas/{id}/estado", citaId).header("Authorization", bearer(tokenMateo))
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CANCELADA\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/citas/{id}/estado", citaId).header("Authorization", bearer(tokenMateo))
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"COMPLETADA\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/clientes").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));

        mvc.perform(get("/api/publico/salones/{slug}", slugB))
                .andExpect(status().isOk()).andExpect(jsonPath("$.servicios", hasSize(1)))
                .andExpect(jsonPath("$.estilistas", hasSize(1)));
        disponibilidad(slugB, corte, mateo, dia).andExpect(status().isNotFound());
        reservar(slugB, corte, mateo, dia, "12:00", "Ajeno", "300 111 2236")
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/clientes").header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));

        disponibilidad(slugA, corte, mateo, LocalDate.now(clock).minusDays(1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.horariosLibres", hasSize(0)));
        LocalDate domingo = dia;
        while (domingo.getDayOfWeek() != DayOfWeek.SUNDAY) { domingo = domingo.plusDays(1); }
        disponibilidad(slugA, corte, mateo, domingo)
                .andExpect(status().isOk()).andExpect(jsonPath("$.horariosLibres", hasSize(0)));
        mvc.perform(get("/api/publico/salones/{slug}/disponibilidad", slugA)
                .param("servicioId", String.valueOf(corte)).param("fecha", dia.toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/publico/salones/{slug}/disponibilidad", slugA)
                .param("estilistaId", String.valueOf(mateo)).param("fecha", dia.toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/publico/salones/{slug}/disponibilidad", slugA)
                .param("servicioId", String.valueOf(corte)).param("estilistaId", String.valueOf(mateo)))
                .andExpect(status().isBadRequest());
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

    private long crearEstilista(String token, String nombre, String email, String ids) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/estilistas").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"" + nombre + "\",\"email\":\"" + email
                        + "\",\"password\":\"estilista123\",\"servicioIds\":[" + ids + "]}"))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private String login(String email) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"estilista123\"}"))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(resultado.getResponse().getContentAsString()).get("token").asString();
    }

    private org.springframework.test.web.servlet.ResultActions disponibilidad(String slug, long servicio,
            long estilista, LocalDate fecha) throws Exception {
        return mvc.perform(get("/api/publico/salones/{slug}/disponibilidad", slug)
                .param("servicioId", String.valueOf(servicio))
                .param("estilistaId", String.valueOf(estilista))
                .param("fecha", fecha.toString()));
    }

    private org.springframework.test.web.servlet.ResultActions reservar(String slug, long servicio,
            long estilista, LocalDate dia, String hora, String cliente, String telefono) throws Exception {
        String cuerpo = """
                {"clienteNombre":"%s","clienteTelefono":"%s","servicioId":%d,
                 "estilistaId":%d,"inicio":"%sT%s:00"}
                """.formatted(cliente, telefono, servicio, estilista, dia, hora);
        return mvc.perform(post("/api/publico/salones/{slug}/reservas", slug)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private LocalDate diaLaboral() {
        LocalDate dia = LocalDate.now(clock).plusDays(2);
        while (dia.getDayOfWeek() == DayOfWeek.SUNDAY) { dia = dia.plusDays(1); }
        return dia;
    }

    private String bearer(String token) { return "Bearer " + token; }
}
