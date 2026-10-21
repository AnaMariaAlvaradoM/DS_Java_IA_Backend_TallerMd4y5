package com.devsenior.sillon;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
import com.devsenior.sillon.auth.*;
import com.devsenior.sillon.cita.CitaRequest;
import com.devsenior.sillon.cliente.ClienteRepository;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.salon.SalonResponse;
import com.devsenior.sillon.servicio.ServicioRequest;
import com.devsenior.sillon.servicio.ServicioService;
import com.devsenior.sillon.usuario.*;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
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
class CorreccionesAuditoriaIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AuthService auth;
    @Autowired ServicioService servicios;
    @Autowired EstilistaService estilistas;
    @Autowired UsuarioRepository usuarios;
    @Autowired SalonRepository salones;
    @Autowired ClienteRepository clientes;
    @Autowired PasswordEncoder passwords;
    @Autowired Clock reloj;
    private AuthResponse dueno;
    private Long servicioId;

    @BeforeEach void preparar() {
        dueno = auth.registrar(registro("secreta123"));
        servicioId = servicios.crear(dueno.salonId(), new ServicioRequest("Corte", null, 45, 35000L, true)).id();
    }

    @Test void adminListaTodosLosSalonesConDtoSinSecretos() throws Exception {
        var otro = auth.registrar(registro("secreta123"));
        Usuario admin = usuarios.findById(dueno.usuarioId()).orElseThrow(); admin.setRol(Rol.ADMIN); usuarios.saveAndFlush(admin);
        var resultado = mvc.perform(get("/api/admin/salones").header("Authorization", bearer(dueno)))
                .andExpect(status().isOk()).andReturn();
        var respuesta = json.readValue(resultado.getResponse().getContentAsString(), SalonResponse[].class);
        var ids = Arrays.stream(respuesta).map(SalonResponse::id).collect(Collectors.toSet());
        assertEquals(salones.findAll().stream().map(salon -> salon.getId()).collect(Collectors.toSet()), ids);
        assertTrue(ids.containsAll(List.of(dueno.salonId(), otro.salonId())));
        assertFalse(resultado.getResponse().getContentAsString().contains("password"));
        assertFalse(resultado.getResponse().getContentAsString().contains("stripeSuscripcionId"));
    }

    @ParameterizedTest @ValueSource(strings = {"DUENO", "ESTILISTA"})
    void otrosRolesNoPuedenListarTodosLosSalones(String rol) throws Exception {
        Usuario usuario = usuarios.findById(dueno.usuarioId()).orElseThrow(); usuario.setRol(Rol.valueOf(rol)); usuarios.saveAndFlush(usuario);
        mvc.perform(get("/api/admin/salones").header("Authorization", bearer(dueno)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value(403));
    }

    @Test void listadoAdminSinTokenEs401() throws Exception {
        mvc.perform(get("/api/admin/salones")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value(401));
    }

    @ParameterizedTest @ValueSource(strings = {"ascii73", "unicode37", "mixta73"})
    void registroRechazaMasDe72BytesSinCrearSalon(String caso) throws Exception {
        long cantidad = salones.count();
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(registro(invalida(caso)))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje", containsString("password")))
                .andExpect(jsonPath("$.mensaje", containsString("72 bytes")));
        assertEquals(cantidad, salones.count());
    }

    @ParameterizedTest @ValueSource(strings = {"ascii73", "unicode37", "mixta73"})
    void estilistaRechazaMasDe72BytesSinCrearlo(String caso) throws Exception {
        long cantidad = usuarios.count();
        var request = new EstilistaRequest("Sara", email(), invalida(caso), List.of(servicioId));
        mvc.perform(post("/api/estilistas").header("Authorization", bearer(dueno)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje", containsString("72 bytes")));
        assertEquals(cantidad, usuarios.count());
    }

    @ParameterizedTest @ValueSource(strings = {"ascii72", "unicode36", "mixta72"})
    void registroAceptaExactamente72BytesYPermiteLogin(String caso) throws Exception {
        String password = valida(caso); RegistroRequest request = registro(password);
        var resultado = mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn();
        AuthResponse registrada = json.readValue(resultado.getResponse().getContentAsString(), AuthResponse.class);
        assertTrue(passwords.matches(password, usuarios.findById(registrada.usuarioId()).orElseThrow().getPassword()));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new LoginRequest(request.email(), password)))).andExpect(status().isOk());
    }

    @ParameterizedTest @ValueSource(strings = {"ascii72", "unicode36", "mixta72"})
    void estilistaAceptaExactamente72Bytes(String caso) throws Exception {
        var request = new EstilistaRequest("Sara", email(), valida(caso), List.of(servicioId));
        mvc.perform(post("/api/estilistas").header("Authorization", bearer(dueno)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andExpect(status().isCreated());
    }

    @Test void loginConPasswordUnicodeInvalidaConserva401Generico() throws Exception {
        String email = usuarios.findById(dueno.usuarioId()).orElseThrow().getEmail();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new LoginRequest(email, "á".repeat(37)))))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
    }

    @ParameterizedTest @ValueSource(strings = {"privada", "publica"})
    void nombreDeClienteLargoSeGuardaSinLimiteNuevo(String ruta) throws Exception {
        var sara = estilistas.crear(dueno.salonId(), new EstilistaRequest("Sara", email(), "secreta123", List.of(servicioId)));
        LocalDate dia = LocalDate.now(reloj).plusDays(1);
        while (dia.getDayOfWeek() == DayOfWeek.SUNDAY) { dia = dia.plusDays(1); }
        String nombre = "Á".repeat(256);
        CitaRequest request = new CitaRequest(nombre, "3103334455", servicioId, sara.id(), dia.atTime(10, 0), null);
        String path = ruta.equals("privada") ? "/api/citas" : "/api/publico/salones/" + dueno.salonSlug() + "/reservas";
        var llamada = post(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request));
        if (ruta.equals("privada")) { llamada.header("Authorization", bearer(dueno)); }
        mvc.perform(llamada).andExpect(status().isCreated()).andExpect(jsonPath("$.clienteNombre").value(nombre));
        assertEquals(nombre, clientes.findBySalonIdAndTelefono(dueno.salonId(), "3103334455").orElseThrow().getNombre());
    }

    private String invalida(String caso) {
        return switch (caso) { case "ascii73" -> "a".repeat(73); case "unicode37" -> "á".repeat(37); default -> "a".repeat(71) + "á"; };
    }
    private String valida(String caso) {
        return switch (caso) { case "ascii72" -> "a".repeat(72); case "unicode36" -> "á".repeat(36); default -> "a".repeat(70) + "á"; };
    }
    private RegistroRequest registro(String password) {
        return new RegistroRequest("Auditoría " + UUID.randomUUID(), "Medellín", "Calle prueba", "6045551234", "Camilo", email(), password);
    }
    private String email() { return "auditoria." + UUID.randomUUID() + "@ejemplo.test"; }
    private String bearer(AuthResponse respuesta) { return "Bearer " + respuesta.token(); }
}
