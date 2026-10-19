package com.devsenior.sillon.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devsenior.sillon.auth.AuthResponse;
import com.devsenior.sillon.auth.AuthService;
import com.devsenior.sillon.auth.LoginRequest;
import com.devsenior.sillon.auth.RegistroRequest;
import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.cliente.ClienteService;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.usuario.EstilistaRequest;
import com.devsenior.sillon.usuario.EstilistaResponse;
import com.devsenior.sillon.usuario.EstilistaService;
import com.devsenior.sillon.usuario.UsuarioRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogoEquipoIntegracionTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private AuthService auth;
    @Autowired private ServicioService servicios;
    @Autowired private EstilistaService estilistas;
    @Autowired private ClienteService clientes;
    @Autowired private SalonRepository salones;
    @Autowired private UsuarioRepository usuarios;

    @Test
    void creaServicioActivoPorDefectoYPermiteEditar() throws Exception {
        AuthResponse dueno = dueno();
        var resultado = mvc.perform(post("/api/servicios").header("Authorization", bearer(dueno))
                        .contentType(MediaType.APPLICATION_JSON).content(servicioJson(45, 35000)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.precio").value(35000))
                .andExpect(jsonPath("$.duracionMinutos").value(45)).andReturn();
        ServicioResponse servicio = mapper.readValue(resultado.getResponse().getContentAsString(), ServicioResponse.class);
        mvc.perform(put("/api/servicios/{id}", servicio.id()).header("Authorization", bearer(dueno))
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of(
                                "nombre", "Corte actualizado", "duracionMinutos", 30, "precio", 40000, "activo", false))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.precio").value(40000));
    }

    @ParameterizedTest
    @ValueSource(ints = {14, 481})
    void duracionFueraDeRangoEs400(int duracion) throws Exception {
        AuthResponse dueno = dueno();
        mvc.perform(post("/api/servicios").header("Authorization", bearer(dueno))
                        .contentType(MediaType.APPLICATION_JSON).content(servicioJson(duracion, 35000)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void precioNoPositivoEs400(int precio) throws Exception {
        AuthResponse dueno = dueno();
        mvc.perform(post("/api/servicios").header("Authorization", bearer(dueno))
                        .contentType(MediaType.APPLICATION_JSON).content(servicioJson(45, precio)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void precioDecimalNoSeTruncaSilenciosamente() throws Exception {
        AuthResponse dueno = dueno();
        mvc.perform(post("/api/servicios").header("Authorization", bearer(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Corte\",\"duracionMinutos\":45,\"precio\":35000.5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void serviciosAjenosNoAparecenYNoSePuedenEditar() throws Exception {
        AuthResponse a = dueno();
        AuthResponse b = dueno();
        ServicioResponse servicio = corte(a);
        mvc.perform(get("/api/servicios").header("Authorization", bearer(b)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(put("/api/servicios/{id}", servicio.id()).header("Authorization", bearer(b))
                        .contentType(MediaType.APPLICATION_JSON).content(servicioJson(45, 35000)))
                .andExpect(status().isNotFound());
    }

    @Test
    void creaEstilistaConServiciosSinExponerPassword() throws Exception {
        AuthResponse dueno = dueno();
        ServicioResponse corte = corte(dueno);
        ServicioResponse barba = servicios.crear(dueno.salonId(), new ServicioRequest("Barba", null, 30, 20000L, true));
        String correo = email();
        mvc.perform(post("/api/estilistas").header("Authorization", bearer(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estilistaJson(correo, List.of(corte.id(), barba.id()))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.servicios.length()").value(2))
                .andExpect(jsonPath("$.servicios", org.hamcrest.Matchers.hasItems("Corte", "Barba")))
                .andExpect(jsonPath("$.password").doesNotExist());
        var usuario = usuarios.findByEmail(correo).orElseThrow();
        assertTrue(usuario.ofreceServicio(corte.id()));
        assertTrue(usuario.ofreceServicio(barba.id()));
    }

    @Test
    void limiteGratisSeValidaAntesQueCorreoYServicios() throws Exception {
        AuthResponse dueno = dueno();
        ServicioResponse corte = corte(dueno);
        crearEstilista(dueno, corte);
        crearEstilista(dueno, corte);
        String correoExistente = usuarios.findById(dueno.usuarioId()).orElseThrow().getEmail();
        mvc.perform(post("/api/estilistas").header("Authorization", bearer(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estilistaJson(correoExistente, List.of(Long.MAX_VALUE))))
                .andExpect(status().isPaymentRequired());
        mvc.perform(get("/api/estilistas").header("Authorization", bearer(dueno)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void planProPermiteTercerEstilista() throws Exception {
        AuthResponse dueno = dueno();
        ServicioResponse corte = corte(dueno);
        crearEstilista(dueno, corte);
        crearEstilista(dueno, corte);
        var salon = salones.findById(dueno.salonId()).orElseThrow();
        salon.setPlan(Plan.PRO);
        salones.saveAndFlush(salon);
        mvc.perform(post("/api/estilistas").header("Authorization", bearer(dueno))
                        .contentType(MediaType.APPLICATION_JSON).content(estilistaJson(email(), List.of(corte.id()))))
                .andExpect(status().isCreated());
    }

    @Test
    void correoUnicoSeValidaAntesDeServicios() throws Exception {
        AuthResponse a = dueno();
        AuthResponse b = dueno();
        String correo = usuarios.findById(b.usuarioId()).orElseThrow().getEmail();
        mvc.perform(post("/api/estilistas").header("Authorization", bearer(a))
                        .contentType(MediaType.APPLICATION_JSON).content(estilistaJson(correo, List.of(Long.MAX_VALUE))))
                .andExpect(status().isConflict());
    }

    @Test
    void servicioDeOtroSalonNoSePuedeAsignar() throws Exception {
        AuthResponse a = dueno();
        AuthResponse b = dueno();
        ServicioResponse ajeno = corte(b);
        mvc.perform(post("/api/estilistas").header("Authorization", bearer(a))
                        .contentType(MediaType.APPLICATION_JSON).content(estilistaJson(email(), List.of(ajeno.id()))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/estilistas").header("Authorization", bearer(a)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void estilistaNecesitaAlMenosUnServicio() throws Exception {
        AuthResponse dueno = dueno();
        mvc.perform(post("/api/estilistas").header("Authorization", bearer(dueno))
                        .contentType(MediaType.APPLICATION_JSON).content(estilistaJson(email(), List.of())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void estilistaPuedeLeerServiciosPeroNoCrearlosNiVerClientes() throws Exception {
        AuthResponse dueno = dueno();
        ServicioResponse corte = corte(dueno);
        EstilistaResponse estilista = crearEstilista(dueno, corte);
        AuthResponse sesion = auth.login(new LoginRequest(estilista.email(), "secreta123"));
        mvc.perform(get("/api/servicios").header("Authorization", bearer(sesion)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(post("/api/servicios").header("Authorization", bearer(sesion))
                        .contentType(MediaType.APPLICATION_JSON).content(servicioJson(45, 35000)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/clientes").header("Authorization", bearer(sesion)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/estilistas").header("Authorization", bearer(sesion)))
                .andExpect(status().isForbidden());
    }

    @Test
    void clienteSeReutilizaPorTelefonoNormalizadoSoloDentroDelSalon() throws Exception {
        AuthResponse a = dueno();
        AuthResponse b = dueno();
        Cliente primero = clientes.buscarOCrear(a.salonId(), "Valentina", "+57 (300) 111-2233");
        Cliente repetido = clientes.buscarOCrear(a.salonId(), "Valentina", "+573001112233");
        assertEquals(primero.getId(), repetido.getId());
        mvc.perform(get("/api/clientes").header("Authorization", bearer(b)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        Cliente otroSalon = clientes.buscarOCrear(b.salonId(), "Otra persona", "+573001112233");
        assertNotEquals(primero.getId(), otroSalon.getId());
        mvc.perform(get("/api/clientes").header("Authorization", bearer(a)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].telefono").value("+573001112233"))
                .andExpect(jsonPath("$[0].nombre").value("Valentina"));
    }

    private AuthResponse dueno() {
        return auth.registrar(new RegistroRequest("Salón " + UUID.randomUUID(), "Bogotá", "Calle 1", "3001112233",
                "Dueño", email(), "secreta123"));
    }

    private ServicioResponse corte(AuthResponse dueno) {
        return servicios.crear(dueno.salonId(), new ServicioRequest("Corte", null, 45, 35000L, true));
    }

    private EstilistaResponse crearEstilista(AuthResponse dueno, ServicioResponse servicio) {
        return estilistas.crear(dueno.salonId(), new EstilistaRequest("Mateo", email(), "secreta123", List.of(servicio.id())));
    }

    private String servicioJson(int duracion, int precio) throws Exception {
        return mapper.writeValueAsString(Map.of("nombre", "Corte", "duracionMinutos", duracion, "precio", precio));
    }

    private String estilistaJson(String email, List<Long> servicios) throws Exception {
        return mapper.writeValueAsString(Map.of("nombre", "Mateo", "email", email, "password", "secreta123", "servicioIds", servicios));
    }

    private String email() { return "usuario." + UUID.randomUUID() + "@ejemplo.test"; }
    private String bearer(AuthResponse auth) { return "Bearer " + auth.token(); }
}
