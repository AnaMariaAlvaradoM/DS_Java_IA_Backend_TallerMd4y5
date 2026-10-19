package com.devsenior.sillon.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.seguridad.JwtService;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthIntegracionTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private SalonRepository salones;
    @Autowired private PasswordEncoder passwords;
    @Autowired private JwtService jwt;

    @Test
    void registroCreaDuenoSalonGratisYPasswordBCrypt() throws Exception {
        String email = emailUnico();
        AuthResponse auth = registrar(email);
        assertEquals("Bearer", auth.tipo());
        assertEquals(60, auth.expiraEnMinutos());
        assertEquals(Rol.DUENO, auth.rol());
        assertTrue(auth.salonSlug().startsWith("barberia-la-navaja-"));
        assertEquals(email, jwt.obtenerEmail(auth.token()));
        Usuario usuario = usuarios.findById(auth.usuarioId()).orElseThrow();
        assertNotEquals("secreta123", usuario.getPassword());
        assertTrue(passwords.matches("secreta123", usuario.getPassword()));
        mvc.perform(get("/api/salon").header("Authorization", bearer(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(auth.salonId()))
                .andExpect(jsonPath("$.plan").value("GRATIS"))
                .andExpect(jsonPath("$.creditosIa").value(10))
                .andExpect(jsonPath("$.configuracion.horaApertura").value("08:00:00"))
                .andExpect(jsonPath("$.configuracion.horaCierre").value("19:00:00"))
                .andExpect(jsonPath("$.configuracion.abreDomingos").value(false))
                .andExpect(jsonPath("$.configuracion.intervaloMinutos").value(30))
                .andExpect(jsonPath("$.fechaCreacion").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void correoRepetidoNormalizadoNoCreaOtroSalon() throws Exception {
        String email = emailUnico();
        registrar(email);
        long cantidad = salones.count();
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(datosRegistro("  " + email.toUpperCase() + "  "))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value(409));
        assertEquals(cantidad, salones.count());
    }

    @Test
    void registroInvalidoIndicaCampos() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"corta\",\"email\":\"invalido\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("password")))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("salonNombre")))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("email")));
    }

    @Test
    void nombreQueGeneraSlugVacioEs400() throws Exception {
        Map<String, String> datos = new java.util.HashMap<>(datosRegistro(emailUnico()));
        datos.put("salonNombre", "!!!");
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(datos)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("salonNombre")));
    }

    @Test
    void sinTokenResponde401UniformeSinSesionNiRedireccion() throws Exception {
        mvc.perform(get("/api/salon"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$.codigo").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.fecha").isNotEmpty())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void tokenInvalidoResponde401() throws Exception {
        mvc.perform(get("/api/salon").header("Authorization", "Bearer invalido"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value(401));
    }

    @Test
    void loginCorrectoNormalizaCorreo() throws Exception {
        String email = emailUnico();
        AuthResponse registrado = registrar(email);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", " " + email.toUpperCase() + " ",
                                "password", "secreta123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(registrado.usuarioId()))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"));
    }

    @Test
    void credencialesIncorrectasNoRevelanSiElCorreoExiste() throws Exception {
        String email = emailUnico();
        registrar(email);
        for (String correo : new String[] {email, emailUnico()}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(Map.of("email", correo, "password", "incorrecta"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
        }
    }

    @Test
    void configuraSoloSuSalonYConservaElOtro() throws Exception {
        AuthResponse salonA = registrar(emailUnico());
        AuthResponse salonB = registrar(emailUnico());
        mvc.perform(put("/api/salon/configuracion").header("Authorization", bearer(salonA))
                        .contentType(MediaType.APPLICATION_JSON).content(configuracion("09:00", "18:00", 15)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(salonA.salonId()))
                .andExpect(jsonPath("$.configuracion.horaApertura").value("09:00:00"));
        mvc.perform(get("/api/salon").header("Authorization", bearer(salonB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(salonB.salonId()))
                .andExpect(jsonPath("$.configuracion.horaApertura").value("08:00:00"));
    }

    @ParameterizedTest
    @ValueSource(ints = {14, 61})
    void intervaloFueraDeRangoEs400(int intervalo) throws Exception {
        AuthResponse auth = registrar(emailUnico());
        mvc.perform(put("/api/salon/configuracion").header("Authorization", bearer(auth))
                        .contentType(MediaType.APPLICATION_JSON).content(configuracion("08:00", "19:00", intervalo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("intervaloMinutos")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"08:00", "07:00"})
    void cierreIgualOAnteriorALaAperturaEs409(String cierre) throws Exception {
        AuthResponse auth = registrar(emailUnico());
        mvc.perform(put("/api/salon/configuracion").header("Authorization", bearer(auth))
                        .contentType(MediaType.APPLICATION_JSON).content(configuracion("08:00", cierre, 30)))
                .andExpect(status().isConflict());
    }

    @Test
    void configuracionIncompletaEs400() throws Exception {
        AuthResponse auth = registrar(emailUnico());
        mvc.perform(put("/api/salon/configuracion").header("Authorization", bearer(auth))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ESTILISTA", "ADMIN"})
    void soloDuenoPuedeConsultarSuSalon(String rol) throws Exception {
        AuthResponse auth = registrar(emailUnico());
        Usuario usuario = usuarios.findById(auth.usuarioId()).orElseThrow();
        usuario.setRol(Rol.valueOf(rol));
        usuarios.saveAndFlush(usuario);
        mvc.perform(get("/api/salon").header("Authorization", "Bearer " + jwt.generar(usuario)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void duenoNoPuedeEntrarAAdministracion() throws Exception {
        AuthResponse auth = registrar(emailUnico());
        mvc.perform(get("/api/admin/salones").header("Authorization", bearer(auth)))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioDesactivadoNoPuedeUsarTokenEmitidoAntes() throws Exception {
        AuthResponse auth = registrar(emailUnico());
        Usuario usuario = usuarios.findById(auth.usuarioId()).orElseThrow();
        usuario.setActivo(false);
        usuarios.saveAndFlush(usuario);
        mvc.perform(get("/api/salon").header("Authorization", bearer(auth)))
                .andExpect(status().isUnauthorized());
    }

    private AuthResponse registrar(String email) throws Exception {
        var resultado = mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(datosRegistro(email))))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readValue(resultado.getResponse().getContentAsString(), AuthResponse.class);
    }

    private Map<String, String> datosRegistro(String email) {
        return Map.of("salonNombre", "Barbería La Navaja " + UUID.randomUUID(), "ciudad", "Medellín",
                "direccion", "Calle 10 # 43-20", "telefono", "604 555 1234", "nombre", "Camilo Ríos",
                "email", email, "password", "secreta123");
    }

    private String configuracion(String apertura, String cierre, int intervalo) throws Exception {
        return mapper.writeValueAsString(Map.of("horaApertura", apertura, "horaCierre", cierre,
                "abreDomingos", false, "intervaloMinutos", intervalo));
    }

    private String emailUnico() { return "camilo." + UUID.randomUUID() + "@navaja.co"; }
    private String bearer(AuthResponse auth) { return "Bearer " + auth.token(); }
}
