package com.devsenior.sillon.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.SalonRepository;
import com.jayway.jsonpath.JsonPath;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EstilistaIntegracionTest {

    private static final String RUTA = "/api/estilistas";
    private static final String PASSWORD_PRUEBA = "solo-para-pruebas";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private SalonRepository salones;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private Dueno dueno;
    private int corteId;

    @BeforeEach
    void prepararSalon() throws Exception {
        dueno = registrarDueno();
        corteId = crearServicio(dueno, "Corte");
    }

    @Test
    void creaElPrimerEstilistaConLosCamposDelContrato() throws Exception {
        int barbaId = crearServicio(dueno, "Barba");
        String email = correo();

        crear(dueno.token(), "Mateo", email, corteId + "," + barbaId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Mateo"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.servicios").value(containsInAnyOrder("Corte", "Barba")));
    }

    @Test
    void permiteElSegundoEstilistaEnGratis() throws Exception {
        crear(dueno.token(), "Mateo", correo(), ids(corteId)).andExpect(status().isCreated());
        crear(dueno.token(), "Sara", correo(), ids(corteId)).andExpect(status().isCreated());
    }

    @Test
    void bloqueaElTerceroEnGratisAntesDeRevisarElCorreoYLosServicios() throws Exception {
        crear(dueno.token(), "Mateo", correo(), ids(corteId)).andExpect(status().isCreated());
        crear(dueno.token(), "Sara", correo(), ids(corteId)).andExpect(status().isCreated());

        crear(dueno.token(), "Ana", dueno.email(), "-1")
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.codigo").value(402));
    }

    @Test
    void permiteElTerceroEnPro() throws Exception {
        salones.findById(dueno.salonId()).orElseThrow().setPlan(Plan.PRO);

        crear(dueno.token(), "Mateo", correo(), ids(corteId)).andExpect(status().isCreated());
        crear(dueno.token(), "Sara", correo(), ids(corteId)).andExpect(status().isCreated());
        crear(dueno.token(), "Ana", correo(), ids(corteId)).andExpect(status().isCreated());
    }

    @Test
    void rechazaUnCorreoDeOtroSalonAntesDeBuscarLosServicios() throws Exception {
        Dueno otro = registrarDueno();

        crear(dueno.token(), "Mateo", otro.email().toUpperCase(Locale.ROOT), "-1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value(409));
    }

    @Test
    void rechazaUnaListaConUnServicioAjenoOInexistente() throws Exception {
        Dueno otro = registrarDueno();
        int ajeno = crearServicio(otro, "Tinte");
        String email = correo();

        for (int id : new int[] {ajeno, -1}) {
            crear(dueno.token(), "Mateo", email, corteId + "," + id)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value(404));
            assertThat(usuarios.existsByEmail(email)).isFalse();
        }
    }

    @Test
    void rechazaUnaListaVacia() throws Exception {
        crear(dueno.token(), "Mateo", correo(), "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(containsString("servicioIds")));
    }

    @Test
    void rechazaUnaPasswordQueSuperaElLimiteDeBytesDeBCrypt() throws Exception {
        mockMvc.perform(post(RUTA).header("Authorization", "Bearer " + dueno.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"Mateo","email":"%s","password":"%s","servicioIds":[%s]}
                        """.formatted(correo(), "\u00e1".repeat(37), corteId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(containsString("password")));
    }

    @Test
    void persisteRolEstadoBCryptYLaRelacionConServicios() throws Exception {
        int barbaId = crearServicio(dueno, "Barba");
        String email = correo();
        crear(dueno.token(), "Mateo", email, ids(corteId)).andExpect(status().isCreated());
        entityManager.flush();
        entityManager.clear();

        Usuario estilista = usuarios.findByEmail(email).orElseThrow();
        assertThat(estilista.getRol()).isEqualTo(Rol.ESTILISTA);
        assertThat(estilista.isActivo()).isTrue();
        assertThat(estilista.getPassword()).startsWith("$2").isNotEqualTo(PASSWORD_PRUEBA);
        assertThat(passwordEncoder.matches(PASSWORD_PRUEBA, estilista.getPassword())).isTrue();
        assertThat(estilista.ofreceServicio((long) corteId)).isTrue();
        assertThat(estilista.ofreceServicio((long) barbaId)).isFalse();
    }

    @Test
    void listaSoloEstilistasDelSalonAutenticado() throws Exception {
        Dueno otro = registrarDueno();
        int ajeno = crearServicio(otro, "Tinte");
        crear(dueno.token(), "Mateo", correo(), ids(corteId)).andExpect(status().isCreated());
        crear(otro.token(), "Sara", correo(), ids(ajeno)).andExpect(status().isCreated());
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get(RUTA).header("Authorization", "Bearer " + dueno.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].length()").value(5))
                .andExpect(jsonPath("$[0].nombre").value("Mateo"))
                .andExpect(jsonPath("$[0].servicios[0]").value("Corte"));
    }

    @Test
    void unEstilistaNoPuedeCrearNiListarEstilistas() throws Exception {
        String email = correo();
        crear(dueno.token(), "Mateo", email, ids(corteId)).andExpect(status().isCreated());
        String respuesta = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, PASSWORD_PRUEBA)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(respuesta, "$.token");

        crear(token, "Sara", correo(), ids(corteId)).andExpect(status().isForbidden());
        mockMvc.perform(get(RUTA).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private Dueno registrarDueno() throws Exception {
        String email = correo();
        String respuesta = mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"salonNombre":"Salon de prueba","ciudad":"Bogota","direccion":"Calle 10",
                         "telefono":"5551234","nombre":"Dueno","email":"%s","password":"%s"}
                        """.formatted(email, PASSWORD_PRUEBA)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Number salonId = JsonPath.read(respuesta, "$.salonId");
        return new Dueno(JsonPath.read(respuesta, "$.token"), salonId.longValue(), email);
    }

    private int crearServicio(Dueno propietario, String nombre) throws Exception {
        String respuesta = mockMvc.perform(post("/api/servicios")
                .header("Authorization", "Bearer " + propietario.token()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"%s","duracionMinutos":45,"precio":35000}
                        """.formatted(nombre)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(respuesta, "$.id");
        return id.intValue();
    }

    private ResultActions crear(String token, String nombre, String email, String servicioIds) throws Exception {
        return mockMvc.perform(post(RUTA).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"%s","email":"%s","password":"%s","servicioIds":[%s]}
                        """.formatted(nombre, email, PASSWORD_PRUEBA, servicioIds)));
    }

    private String ids(int id) {
        return String.valueOf(id);
    }

    private String correo() {
        return UUID.randomUUID() + "@estilistas.example";
    }

    private record Dueno(String token, long salonId, String email) {
    }
}
