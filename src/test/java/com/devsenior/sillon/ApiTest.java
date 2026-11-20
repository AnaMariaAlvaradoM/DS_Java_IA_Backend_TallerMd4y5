package com.devsenior.sillon;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:api;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.jpa.hibernate.ddl-auto=create-drop"
        })
@AutoConfigureMockMvc
class ApiTest {
    @Autowired MockMvc api;
    @Autowired ObjectMapper json;

    @Test
    void solicitaCredencialesParaDatosPrivados() throws Exception {
        api.perform(get("/api/salon")).andExpect(status().isUnauthorized());
    }

    @Test
    void erroresNombranTodosLosCampos() throws Exception {
        api.perform(post("/api/auth/registro").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("email")))
                .andExpect(
                        jsonPath("$.mensaje")
                                .value(org.hamcrest.Matchers.containsString("password")));
    }

    @Test
    void creaSesionYConservaCreditosIniciales() throws Exception {
        var registro =
                Map.of(
                        "salonNombre",
                        "Luz",
                        "nombre",
                        "Rosa",
                        "email",
                        "rosa@example.test",
                        "password",
                        "prueba-local",
                        "ciudad",
                        "Cali",
                        "direccion",
                        "Calle 2",
                        "telefono",
                        "3001234567");
        var respuesta =
                api.perform(
                                post("/api/auth/registro")
                                        .contentType("application/json")
                                        .content(json.writeValueAsBytes(registro)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.rol").value("DUENO"))
                        .andReturn();
        var token =
                json.readTree(respuesta.getResponse().getContentAsByteArray())
                        .get("token")
                        .asText();
        api.perform(get("/api/salon").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditosIa").value(10))
                .andExpect(jsonPath("$.plan").value("GRATIS"));
        api.perform(get("/api/admin/salones").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        api.perform(
                        post("/api/auth/registro")
                                .contentType("application/json")
                                .content(json.writeValueAsBytes(registro)))
                .andExpect(status().isConflict());
    }
}
