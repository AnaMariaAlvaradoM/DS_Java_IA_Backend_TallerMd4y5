package com.devsenior.sillon;

import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.cliente.ClienteServicio;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.SalonRepositorio;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Fase3IntegracionTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired SalonRepositorio salones;
    @Autowired ClienteServicio clientes;

    @Test
    void catalogoEquipoYPermisosAislados() throws Exception {
        JsonNode duenoA = registrar("A");
        JsonNode duenoB = registrar("B");
        String tokenA = duenoA.get("token").asString();
        String tokenB = duenoB.get("token").asString();
        long corte = crearServicio(tokenA, "Corte", 45, 35000);
        long barba = crearServicio(tokenA, "Barba", 30, 20000);
        long servicioB = crearServicio(tokenB, "Peinado", 60, 40000);

        mvc.perform(post("/api/servicios").header("Authorization", bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON).content(servicioJson("Malo", 5, -1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", org.hamcrest.Matchers.containsString("duracionMinutos")))
                .andExpect(jsonPath("$.mensaje", org.hamcrest.Matchers.containsString("precio")));
        mvc.perform(put("/api/servicios/{id}", corte).header("Authorization", bearer(tokenB))
                .contentType(MediaType.APPLICATION_JSON).content(servicioJson("Otro", 45, 35000)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/servicios/{id}", corte).header("Authorization", bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Corte actualizado\",\"duracionMinutos\":45,\"precio\":36000,\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Corte actualizado"))
                .andExpect(jsonPath("$.activo").value(false));
        mvc.perform(get("/api/servicios").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/servicios").header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));

        String emailMateo = "mateo-" + UUID.randomUUID() + "@ejemplo.co";
        mvc.perform(post("/api/estilistas").header("Authorization", bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(estilistaJson("Mateo", emailMateo, corte + "," + barba)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.servicios", hasSize(2)))
                .andExpect(jsonPath("$.password").doesNotExist());
        String emailSara = "sara-" + UUID.randomUUID() + "@ejemplo.co";
        mvc.perform(post("/api/estilistas").header("Authorization", bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(estilistaJson("Sara", emailSara, String.valueOf(corte))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.servicios", hasSize(1)));
        mvc.perform(post("/api/estilistas").header("Authorization", bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(estilistaJson("Tercero", "tercero-" + UUID.randomUUID() + "@ejemplo.co", String.valueOf(servicioB))))
                .andExpect(status().isPaymentRequired());

        mvc.perform(post("/api/estilistas").header("Authorization", bearer(tokenB))
                .contentType(MediaType.APPLICATION_JSON)
                .content(estilistaJson("Ajeno", "ajeno-" + UUID.randomUUID() + "@ejemplo.co", String.valueOf(corte))))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/estilistas").header("Authorization", bearer(tokenB))
                .contentType(MediaType.APPLICATION_JSON)
                .content(estilistaJson("Sin servicios", "vacio-" + UUID.randomUUID() + "@ejemplo.co", "")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/estilistas").header("Authorization", bearer(tokenB))
                .contentType(MediaType.APPLICATION_JSON)
                .content(estilistaJson("Repetido", emailMateo, String.valueOf(servicioB))))
                .andExpect(status().isConflict());

        salones.findById(duenoA.get("salonId").asLong()).orElseThrow().setPlan(Plan.PRO);
        mvc.perform(post("/api/estilistas").header("Authorization", bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(estilistaJson("Tercero", "pro-" + UUID.randomUUID() + "@ejemplo.co", String.valueOf(corte))))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/estilistas").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(get("/api/estilistas").header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));

        String tokenMateo = login(emailMateo);
        mvc.perform(get("/api/servicios").header("Authorization", bearer(tokenMateo)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(post("/api/servicios").header("Authorization", bearer(tokenMateo))
                .contentType(MediaType.APPLICATION_JSON).content(servicioJson("X", 30, 10000)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/estilistas").header("Authorization", bearer(tokenMateo)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/clientes").header("Authorization", bearer(tokenMateo)))
                .andExpect(status().isForbidden());
    }

    @Test
    void clientesSeNormalizanYSeAislanPorSalon() throws Exception {
        JsonNode duenoA = registrar("Cliente A");
        JsonNode duenoB = registrar("Cliente B");
        long salonA = duenoA.get("salonId").asLong();
        long salonB = duenoB.get("salonId").asLong();
        Cliente primero = clientes.buscarOCrear(salonA, "Valentina", "+57 (300) 111-2233");
        Cliente repetido = clientes.buscarOCrear(salonA, "Otro nombre", "+57 300 111 2233");
        Cliente otroSalon = clientes.buscarOCrear(salonB, "Diana", "+57 300 111 2233");
        org.junit.jupiter.api.Assertions.assertEquals("+573001112233", primero.getTelefono());
        org.junit.jupiter.api.Assertions.assertEquals(primero.getId(), repetido.getId());
        org.junit.jupiter.api.Assertions.assertNotEquals(primero.getId(), otroSalon.getId());
        mvc.perform(get("/api/clientes").header("Authorization", bearer(duenoA.get("token").asString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Valentina"));
        mvc.perform(get("/api/clientes").header("Authorization", bearer(duenoB.get("token").asString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Diana"));
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
                .contentType(MediaType.APPLICATION_JSON).content(servicioJson(nombre, duracion, precio)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.activo").value(true)).andReturn();
        return json.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private String login(String email) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"estilista123\"}"))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(resultado.getResponse().getContentAsString()).get("token").asString();
    }

    private String servicioJson(String nombre, int duracion, int precio) {
        return """
                {"nombre":"%s","duracionMinutos":%d,"precio":%d}
                """.formatted(nombre, duracion, precio);
    }

    private String estilistaJson(String nombre, String email, String ids) {
        return """
                {"nombre":"%s","email":"%s","password":"estilista123","servicioIds":[%s]}
                """.formatted(nombre, email, ids);
    }

    private String bearer(String token) { return "Bearer " + token; }
}
