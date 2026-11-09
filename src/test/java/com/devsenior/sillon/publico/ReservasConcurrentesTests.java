package com.devsenior.sillon.publico;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReservasConcurrentesTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transaccion;
    @Autowired Clock clock;
    private Long salonId;

    @AfterEach
    void limpiarDatosPropios() {
        if (salonId == null) { return; }
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

    @Test
    void dosReservasSimultaneasDelMismoEstilistaSoloCreanUnaCita() throws Exception {
        String sufijo = UUID.randomUUID().toString();
        MvcResult registro = mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("{\"salonNombre\":\"Concurrencia " + sufijo + "\",\"ciudad\":\"Bogotá\","
                        + "\"direccion\":\"Calle 1\",\"telefono\":\"1234567\",\"nombre\":\"Dueño\","
                        + "\"email\":\"dueno-" + sufijo + "@ejemplo.co\",\"password\":\"secreta123\"}"))
                .andExpect(status().isCreated()).andReturn();
        var registrado = json.readTree(registro.getResponse().getContentAsString());
        salonId = registrado.get("salonId").asLong();
        String slug = registrado.get("salonSlug").asString();
        String token = registrado.get("token").asString();
        MvcResult servicio = mvc.perform(post("/api/servicios").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Corte\",\"duracionMinutos\":45,\"precio\":35000}"))
                .andExpect(status().isCreated()).andReturn();
        long servicioId = json.readTree(servicio.getResponse().getContentAsString()).get("id").asLong();
        MvcResult estilista = mvc.perform(post("/api/estilistas").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Mateo\",\"email\":\"mateo-" + sufijo
                        + "@ejemplo.co\",\"password\":\"estilista123\",\"servicioIds\":[" + servicioId + "]}"))
                .andExpect(status().isCreated()).andReturn();
        long estilistaId = json.readTree(estilista.getResponse().getContentAsString()).get("id").asLong();
        LocalDate dia = LocalDate.now(clock).plusDays(2);
        while (dia.getDayOfWeek() == DayOfWeek.SUNDAY) { dia = dia.plusDays(1); }
        String fecha = dia + "T10:00:00";

        CountDownLatch listas = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> primera = ejecutor.submit(() -> reservarAlMismoTiempo(
                    listas, inicio, slug, servicioId, estilistaId, fecha, "Valentina", "3001112233"));
            Future<Integer> segunda = ejecutor.submit(() -> reservarAlMismoTiempo(
                    listas, inicio, slug, servicioId, estilistaId, fecha, "Andrés", "3102223344"));
            assertTrue(listas.await(10, TimeUnit.SECONDS));
            inicio.countDown();
            assertEquals(List.of(201, 409), List.of(primera.get(30, TimeUnit.SECONDS),
                    segunda.get(30, TimeUnit.SECONDS)).stream().sorted().toList());
            assertEquals(1, jdbc.queryForObject("select count(*) from citas where salon_id = ?",
                    Integer.class, salonId));
        } finally {
            inicio.countDown();
            ejecutor.shutdownNow();
        }
    }

    private int reservarAlMismoTiempo(CountDownLatch listas, CountDownLatch inicio, String slug,
            long servicioId, long estilistaId, String fecha, String nombre, String telefono) throws Exception {
        listas.countDown();
        if (!inicio.await(10, TimeUnit.SECONDS)) { throw new IllegalStateException("No comenzó la prueba concurrente"); }
        String cuerpo = "{\"clienteNombre\":\"" + nombre + "\",\"clienteTelefono\":\"" + telefono
                + "\",\"servicioId\":" + servicioId + ",\"estilistaId\":" + estilistaId
                + ",\"inicio\":\"" + fecha + "\"}";
        return mvc.perform(post("/api/publico/salones/{slug}/reservas", slug)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn().getResponse().getStatus();
    }
}
