package com.devsenior.sillon.cita;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RecordatorioHttpIntegracionTests {
    private static final AtomicReference<String> PROMPT = new AtomicReference<>();
    private static final AtomicInteger LLAMADAS = new AtomicInteger();
    private static final HttpServer IA = iniciarSimulador();

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transaccion;
    @Autowired Clock clock;
    private Long salonId;

    @DynamicPropertySource
    static void iaSimulada(DynamicPropertyRegistry propiedades) {
        propiedades.add("sillon.ia.openai.url", () -> "http://127.0.0.1:" + IA.getAddress().getPort() + "/v1");
        propiedades.add("sillon.ia.openai.llave", () -> "clave-ficticia-simulador");
        propiedades.add("sillon.ia.openai.modelo", () -> "modelo-simulado");
    }

    private static HttpServer iniciarSimulador() {
        try {
            HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            servidor.createContext("/v1/chat/completions", solicitud -> {
                LLAMADAS.incrementAndGet();
                String cuerpo = new String(solicitud.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                PROMPT.set(new ObjectMapper().readTree(cuerpo).path("messages").path(0)
                        .path("content").asText());
                byte[] respuesta = ("{\"choices\":[{\"message\":{\"content\":\""
                        + "{\\\"mensaje\\\":\\\"Sara le espera a las 10:30. Confirme respondiendo SÍ.\\\"}"
                        + "\"}}],\"usage\":{\"prompt_tokens\":90,\"completion_tokens\":20}}")
                        .getBytes(StandardCharsets.UTF_8);
                solicitud.getResponseHeaders().set("Content-Type", "application/json");
                solicitud.sendResponseHeaders(200, respuesta.length);
                try (var salida = solicitud.getResponseBody()) { salida.write(respuesta); }
            });
            servidor.start();
            return servidor;
        } catch (Exception error) { throw new IllegalStateException(error); }
    }

    @AfterAll static void detenerSimulador() { IA.stop(0); }

    @AfterEach void limpiar() {
        if (salonId == null) return;
        transaccion.executeWithoutResult(estado -> {
            jdbc.update("delete from citas where salon_id = ?", salonId);
            jdbc.update("delete from clientes where salon_id = ?", salonId);
            jdbc.update("delete from estilista_servicios where estilista_id in "
                    + "(select id from usuarios where salon_id = ?)", salonId);
            jdbc.update("delete from usuarios where salon_id = ?", salonId);
            jdbc.update("delete from servicios where salon_id = ?", salonId);
            jdbc.update("delete from salones where id = ?", salonId);
        });
    }

    @Test void flujoCompletoConSimuladorYPermisoDueno() throws Exception {
        String sufijo = UUID.randomUUID().toString();
        var registro = json.readTree(mvc.perform(post("/api/auth/registro")
                .contentType("application/json")
                .content("{\"salonNombre\":\"Barbería La Navaja " + sufijo + "\","
                        + "\"ciudad\":\"Medellín\",\"direccion\":\"Calle 10 # 43-20\","
                        + "\"telefono\":\"6045551234\",\"nombre\":\"Camilo Ríos\","
                        + "\"email\":\"camilo-" + sufijo + "@ejemplo.co\",\"password\":\"secreta123\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        salonId = registro.path("salonId").asLong();
        String token = registro.path("token").asText();
        long servicioId = json.readTree(mvc.perform(post("/api/servicios")
                .header("Authorization", "Bearer " + token).contentType("application/json")
                .content("{\"nombre\":\"Corte clásico\",\"duracionMinutos\":45,\"precio\":35000}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .path("id").asLong();
        String emailSara = "sara-" + sufijo + "@ejemplo.co";
        long estilistaId = json.readTree(mvc.perform(post("/api/estilistas")
                .header("Authorization", "Bearer " + token).contentType("application/json")
                .content("{\"nombre\":\"Sara Mejía\",\"email\":\"" + emailSara + "\","
                        + "\"password\":\"secreta123\",\"servicioIds\":[" + servicioId + "]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .path("id").asLong();
        LocalDate dia = LocalDate.now(clock).plusDays(2);
        while (dia.getDayOfWeek() == DayOfWeek.SUNDAY) dia = dia.plusDays(1);
        long citaId = json.readTree(mvc.perform(post("/api/citas")
                .header("Authorization", "Bearer " + token).contentType("application/json")
                .content("{\"clienteNombre\":\"Andrés Gómez\",\"clienteTelefono\":\"310 222 3344\","
                        + "\"servicioId\":" + servicioId + ",\"estilistaId\":" + estilistaId + ","
                        + "\"inicio\":\"" + dia + "T10:30:00\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .path("id").asLong();
        var accesoSara = json.readTree(mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"email\":\"" + emailSara + "\",\"password\":\"secreta123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        mvc.perform(post("/api/citas/{id}/recordatorio", citaId)
                .header("Authorization", "Bearer " + accesoSara.path("token").asText()))
                .andExpect(status().isForbidden());
        assertEquals(0, LLAMADAS.get());
        mvc.perform(post("/api/citas/{id}/recordatorio", citaId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citaId").value(citaId))
                .andExpect(jsonPath("$.telefonoCliente").value("3102223344"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("Sara")))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("10:30")))
                .andExpect(jsonPath("$.creditosRestantes").value(9))
                .andExpect(jsonPath("$.factura.proveedor").value("Gemini"))
                .andExpect(jsonPath("$.factura.modelo").value("modelo-simulado"))
                .andExpect(jsonPath("$.factura.tokensEntrada").value(90))
                .andExpect(jsonPath("$.factura.tokensSalida").value(20))
                .andExpect(jsonPath("$.factura.milisegundos").exists());
        assertEquals(1, LLAMADAS.get());
        assertTrue(PROMPT.get().contains("Sara Mejía"));
        assertTrue(PROMPT.get().contains("10:30 a. m."));
        assertTrue(PROMPT.get().contains("\"mensaje\""));
        mvc.perform(get("/api/salon").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.creditosIa").value(9));
    }
}
