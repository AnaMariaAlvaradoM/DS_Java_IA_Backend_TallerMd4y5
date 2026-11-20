package com.devsenior.sillon.seguridad;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/**
 * Pruebas HTTP del contrato de la fase 2 contra el contexto completo.
 * Igual que SillonApplicationTests, necesita el MySQL local encendido.
 * Los correos llevan un sufijo por ejecución para poder correrse muchas veces.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadIntegracionTest {

    private static final String SUFIJO = String.valueOf(System.currentTimeMillis());
    private static final AtomicInteger CONTADOR = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    private String emailUnico() {
        return "camilo." + SUFIJO + "." + CONTADOR.incrementAndGet() + "@navaja.co";
    }

    private String cuerpoRegistro(String email) {
        return """
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
    }

    private String registrarYObtenerToken(String email) throws Exception {
        String respuesta = mockMvc.perform(post("/api/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoRegistro(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.rol").value("DUENO"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    @Test
    void registroValidoResponde201ConToken() throws Exception {
        registrarYObtenerToken(emailUnico());
    }

    @Test
    void registroConCorreoRepetidoResponde409() throws Exception {
        String email = emailUnico();
        registrarYObtenerToken(email);

        mockMvc.perform(post("/api/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoRegistro(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void registroInvalidoResponde400NombrandoEmailYPassword() throws Exception {
        String cuerpo = """
                {
                  "salonNombre": "Barbería La Navaja",
                  "ciudad": "Medellín",
                  "direccion": "Calle 10 # 43-20",
                  "telefono": "604 555 1234",
                  "nombre": "Camilo Ríos",
                  "email": "no-es-un-correo",
                  "password": "corta"
                }
                """;

        mockMvc.perform(post("/api/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("email")))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("password")));
    }

    @Test
    void loginCorrectoResponde200YConContrasenaIncorrecta401() throws Exception {
        String email = emailUnico();
        registrarYObtenerToken(email);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"password\": \"secreta123\"}".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"password\": \"incorrecta\"}".formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401))
                .andExpect(jsonPath("$.mensaje").value("Credenciales inválidas"));
    }

    @Test
    void miSalonSinTokenResponde401ConElFormatoUnicoDeError() throws Exception {
        mockMvc.perform(get("/api/salon"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.fecha").isNotEmpty());
    }

    @Test
    void miSalonConTokenInvalidoResponde401() throws Exception {
        mockMvc.perform(get("/api/salon").header("Authorization", "Bearer token.invalido.xyz"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401));
    }

    @Test
    void miSalonMuestraPlanGratisY10Creditos() throws Exception {
        String token = registrarYObtenerToken(emailUnico());

        mockMvc.perform(get("/api/salon").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("GRATIS"))
                .andExpect(jsonPath("$.creditosIa").value(10))
                .andExpect(jsonPath("$.configuracion.horaApertura").value("08:00:00"))
                .andExpect(jsonPath("$.configuracion.intervaloMinutos").value(30));
    }

    @Test
    void unDuenoEnLaAdministracionRecibe403ConElFormatoUnico() throws Exception {
        String token = registrarYObtenerToken(emailUnico());

        mockMvc.perform(get("/api/admin/salones").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.fecha").isNotEmpty());
    }

    @Test
    void configurarElHorarioValidoResponde200() throws Exception {
        String token = registrarYObtenerToken(emailUnico());

        mockMvc.perform(put("/api/salon/configuracion")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"horaApertura\": \"09:00\", \"horaCierre\": \"18:00\", \"abreDomingos\": true, \"intervaloMinutos\": 30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configuracion.horaApertura").value("09:00:00"))
                .andExpect(jsonPath("$.configuracion.abreDomingos").value(true));
    }

    @Test
    void unHorarioQueAbreDespuesDeCerrarResponde409() throws Exception {
        String token = registrarYObtenerToken(emailUnico());

        mockMvc.perform(put("/api/salon/configuracion")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"horaApertura\": \"20:00\", \"horaCierre\": \"08:00\", \"abreDomingos\": false, \"intervaloMinutos\": 30}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value(409));
    }

    @Test
    void unIntervaloFueraDe15A60Responde400() throws Exception {
        String token = registrarYObtenerToken(emailUnico());

        mockMvc.perform(put("/api/salon/configuracion")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"horaApertura\": \"08:00\", \"horaCierre\": \"19:00\", \"abreDomingos\": false, \"intervaloMinutos\": 90}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("intervaloMinutos")));
    }
}
