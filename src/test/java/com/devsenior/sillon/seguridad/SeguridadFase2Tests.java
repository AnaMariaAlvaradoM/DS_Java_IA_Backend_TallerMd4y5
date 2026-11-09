package com.devsenior.sillon.seguridad;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeguridadFase2Tests {
    @Autowired MockMvc mvc;

    @Test
    void registroLoginPermisosYConfiguracion() throws Exception {
        String email = "dueno-" + UUID.randomUUID() + "@ejemplo.co";
        String registro = """
                {"salonNombre":"Barbería La Navaja","ciudad":"Medellín","direccion":"Calle 10",
                 "telefono":"6045551234","nombre":"Camilo","email":"%s","password":"secreta123"}
                """.formatted(email);

        String cuerpo = mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(registro))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.rol").value("DUENO"))
                .andExpect(jsonPath("$.salonSlug").value("barberia-la-navaja"))
                .andReturn().getResponse().getContentAsString();
        String token = tools.jackson.databind.json.JsonMapper.builder().build().readTree(cuerpo).get("token").asString();

        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(registro))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"salonNombre":"Otro","ciudad":"Bogotá","direccion":"Calle 1","telefono":"1234567",
                         "nombre":"Ana","email":"invalido","password":"corta"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("email")))
                .andExpect(jsonPath("$.mensaje", containsString("password")));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"secreta123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isString());

        mvc.perform(get("/api/salon"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").isString())
                .andExpect(jsonPath("$.fecha").exists());
        mvc.perform(get("/api/salon").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/salon").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("GRATIS"))
                .andExpect(jsonPath("$.creditosIa").value(10))
                .andExpect(jsonPath("$.configuracion.intervaloMinutos").value(30));
        mvc.perform(put("/api/salon/configuracion").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"horaApertura\":\"19:00\",\"horaCierre\":\"08:00\",\"abreDomingos\":false,\"intervaloMinutos\":30}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/salon/configuracion").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"horaApertura\":\"08:00\",\"horaCierre\":\"19:00\",\"abreDomingos\":false,\"intervaloMinutos\":10}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/salon/configuracion").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"horaApertura\":\"09:00\",\"horaCierre\":\"18:00\",\"abreDomingos\":true,\"intervaloMinutos\":45}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configuracion.intervaloMinutos").value(45))
                .andExpect(jsonPath("$.configuracion.abreDomingos").value(true));
        mvc.perform(get("/api/admin/salones").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(403));
    }
}
