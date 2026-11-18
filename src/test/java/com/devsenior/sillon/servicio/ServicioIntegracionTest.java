package com.devsenior.sillon.servicio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/**
 * Pruebas HTTP del catálogo de servicios (SPEC.md §7.7). Necesita el MySQL
 * local encendido, igual que SillonApplicationTests.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ServicioIntegracionTest {

    private static final String SUFIJO = String.valueOf(System.currentTimeMillis());
    private static final AtomicInteger CONTADOR = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    private String registrarYObtenerToken() throws Exception {
        String email = "dueno." + SUFIJO + "." + CONTADOR.incrementAndGet() + "@catalogo.co";
        String cuerpo = """
                {
                  "salonNombre": "Barbería La Navaja",
                  "ciudad": "Medellín",
                  "direccion": "Calle 10 # 43-20",
                  "telefono": "604 555 1234",
                  "nombre": "Camilo Ríos",
                  "email": "%s",
                  "password": "secreta123"
                }
                """.formatted(email);
        String respuesta = mockMvc.perform(post("/api/auth/registro")
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private String crearServicio(String token, String nombre, int duracion, int precio) throws Exception {
        String cuerpo = "{\"nombre\": \"%s\", \"descripcion\": \"Servicio de prueba\", \"duracionMinutos\": %d, \"precio\": %d}"
                .formatted(nombre, duracion, precio);
        return mockMvc.perform(post("/api/servicios")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void crearServicioResponde201ConLosCamposDelContratoYActivoPorDefecto() throws Exception {
        String token = registrarYObtenerToken();

        String respuesta = crearServicio(token, "Corte clásico", 45, 35000);

        org.assertj.core.api.Assertions.assertThat((Integer) JsonPath.read(respuesta, "$.duracionMinutos"))
                .isEqualTo(45);
        org.assertj.core.api.Assertions.assertThat((Integer) JsonPath.read(respuesta, "$.precio")).isEqualTo(35000);
        org.assertj.core.api.Assertions.assertThat((Boolean) JsonPath.read(respuesta, "$.activo")).isTrue();
    }

    @Test
    void unServicioDe5MinutosOConPrecioNegativoResponde400() throws Exception {
        String token = registrarYObtenerToken();

        mockMvc.perform(post("/api/servicios")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"Retoque\", \"duracionMinutos\": 5, \"precio\": 10000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje").value(Matchers.containsString("duracionMinutos")));

        mockMvc.perform(post("/api/servicios")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"Retoque\", \"duracionMinutos\": 30, \"precio\": -1000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(Matchers.containsString("precio")));
    }

    @Test
    void listarDevuelveLosServiciosDelSalon() throws Exception {
        String token = registrarYObtenerToken();
        crearServicio(token, "Corte clásico", 45, 35000);
        crearServicio(token, "Arreglo de barba", 30, 20000);

        mockMvc.perform(get("/api/servicios").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].nombre").value(
                        Matchers.containsInAnyOrder("Corte clásico", "Arreglo de barba")));
    }

    @Test
    void actualizarUnServicioPropioResponde200ConLosNuevosValores() throws Exception {
        String token = registrarYObtenerToken();
        String creado = crearServicio(token, "Corte clásico", 45, 35000);
        int id = JsonPath.read(creado, "$.id");

        mockMvc.perform(put("/api/servicios/" + id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"Corte premium\", \"duracionMinutos\": 60, \"precio\": 50000, \"activo\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Corte premium"))
                .andExpect(jsonPath("$.precio").value(50000))
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void actualizarElServicioDeOtroSalonResponde404() throws Exception {
        String tokenSalonA = registrarYObtenerToken();
        String creado = crearServicio(tokenSalonA, "Corte clásico", 45, 35000);
        int id = JsonPath.read(creado, "$.id");
        String tokenSalonB = registrarYObtenerToken();

        mockMvc.perform(put("/api/servicios/" + id)
                .header("Authorization", "Bearer " + tokenSalonB)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"Robo de servicio\", \"duracionMinutos\": 30, \"precio\": 1000}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value(404));
    }

    @Test
    void listarSinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/servicios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401));
    }
}
