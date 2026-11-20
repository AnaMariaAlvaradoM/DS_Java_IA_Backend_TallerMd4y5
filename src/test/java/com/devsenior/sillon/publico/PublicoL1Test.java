package com.devsenior.sillon.publico;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.cita.Cita;
import com.devsenior.sillon.cita.EstadoCita;
import com.devsenior.sillon.cita.OrigenCita;
import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;

import jakarta.persistence.EntityManager;

@Tag("Layer1")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(PublicoL1Test.RelojDePrueba.class)
class PublicoL1Test {

    private static final String CATALOGO = "/api/publico/salones/{slug}";
    private static final String DISPONIBILIDAD = CATALOGO + "/disponibilidad";
    private static final LocalDate SABADO = LocalDate.of(2026, 11, 21);
    private static final LocalDate LUNES = SABADO.plusDays(2);
    private static final String PASSWORD_PRUEBA = "valor-privado-de-prueba";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;

    private Salon salon;
    private Servicio corte;
    private Usuario mateo;

    @BeforeEach
    void prepararSalon() {
        salon = crearSalon("Salon central");
        corte = crearServicio(salon, "Corte", true);
        mateo = crearEstilista(salon, "Mateo", true, corte);
    }

    @Test
    void catalogoSinTokenDevuelveSoloDatosPublicosActivosDelSalon() throws Exception {
        Servicio inactivo = crearServicio(salon, "Tinte", false);
        mateo.setServicios(Set.of(corte, inactivo));
        crearEstilista(salon, "Estilista inactiva", false, corte);
        Salon otro = crearSalon("Salon ajeno");
        crearEstilista(otro, "Estilista ajeno", true, crearServicio(otro, "Barba", true));
        prepararConsulta();

        mockMvc.perform(get(CATALOGO, salon.getSlug()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "nombre":"Salon central","slug":"%s","ciudad":"Bogota","direccion":"Calle 10",
                          "telefono":"5551234","horario":"08:00 a 12:00 (domingos cerrado)",
                          "servicios":[{"id":%d,"nombre":"Corte","descripcion":"Corte de prueba",
                                       "duracionMinutos":45,"precio":35000}],
                          "estilistas":[{"id":%d,"nombre":"Mateo","servicioIds":[%d]}]
                        }
                        """.formatted(salon.getSlug(), corte.getId(), mateo.getId(), corte.getId()),
                        JsonCompareMode.STRICT))
                .andExpect(content().string(not(containsString("@"))))
                .andExpect(content().string(not(containsString(PASSWORD_PRUEBA))));
    }

    @Test
    void catalogoDeSalonInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get(CATALOGO, "inexistente-" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value(404));
    }

    @Test
    void disponibilidadSinTokenDevuelveElContratoYHorasConSegundos() throws Exception {
        prepararConsulta();
        mockMvc.perform(disponibilidad(corte, mateo, LUNES))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "fecha":"%s","servicio":"Corte","estilista":"Mateo","duracionMinutos":45,
                          "horariosLibres":["08:00:00","08:30:00","09:00:00","09:30:00",
                                            "10:00:00","10:30:00","11:00:00"]
                        }
                        """.formatted(LUNES), JsonCompareMode.STRICT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"servicioId", "estilistaId", "fecha"})
    void disponibilidadRechazaParametrosAusentesOMalFormados(String parametro) throws Exception {
        for (boolean enviarInvalido : new boolean[] {false, true}) {
            var peticion = get(DISPONIBILIDAD, salon.getSlug());
            agregarParametro(peticion, "servicioId", corte.getId().toString(), parametro, enviarInvalido);
            agregarParametro(peticion, "estilistaId", mateo.getId().toString(), parametro, enviarInvalido);
            agregarParametro(peticion, "fecha", LUNES.toString(), parametro, enviarInvalido);
            mockMvc.perform(peticion)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value(400))
                    .andExpect(jsonPath("$.mensaje").value(containsString(parametro)));
        }
    }

    @Test
    void disponibilidadTrataLosRecursosDeOtroSalonComoInexistentes() throws Exception {
        Salon otro = crearSalon("Salon ajeno");
        Servicio barba = crearServicio(otro, "Barba", true);
        Usuario sara = crearEstilista(otro, "Sara", true, barba);
        prepararConsulta();

        mockMvc.perform(disponibilidad(barba, mateo, LUNES))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value(404));
        mockMvc.perform(disponibilidad(corte, sara, LUNES))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value(404));
    }

    @Test
    void disponibilidadDevuelve409ParaServicioInactivoONoOfrecido() throws Exception {
        Servicio inactivo = crearServicio(salon, "Tinte", false);
        Servicio noOfrecido = crearServicio(salon, "Barba", true);
        mateo.setServicios(Set.of(corte, inactivo));
        prepararConsulta();

        mockMvc.perform(disponibilidad(inactivo, mateo, LUNES))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value(409));
        mockMvc.perform(disponibilidad(noOfrecido, mateo, LUNES))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value(409));
    }

    @Test
    void domingoCerradoYDiaPasadoNoTienenTurnosYHoyExcluyeHorasPasadas() throws Exception {
        prepararConsulta();
        for (LocalDate dia : new LocalDate[] {SABADO.minusDays(1), SABADO.plusDays(1)}) {
            mockMvc.perform(disponibilidad(corte, mateo, dia))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.horariosLibres").isEmpty());
        }
        mockMvc.perform(disponibilidad(corte, mateo, SABADO))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"horariosLibres":["08:30:00","09:00:00","09:30:00","10:00:00","10:30:00","11:00:00"]}
                        """, JsonCompareMode.LENIENT));
    }

    @Test
    void soloBloqueanCitasOcupadasDelEstilistaYSePermitenLimitesConsecutivos() throws Exception {
        crearCita(corte, mateo, LocalTime.of(8, 45), EstadoCita.CONFIRMADA);
        crearCita(corte, mateo, LocalTime.of(10, 0), EstadoCita.PENDIENTE);
        crearCita(corte, mateo, LocalTime.of(11, 0), EstadoCita.CANCELADA);
        Usuario companera = crearEstilista(salon, "Ana", true, corte);
        crearCita(corte, companera, LocalTime.of(8, 0), EstadoCita.CONFIRMADA);
        Salon otro = crearSalon("Salon ajeno");
        Servicio otroCorte = crearServicio(otro, "Otro corte", true);
        Usuario otroEstilista = crearEstilista(otro, "Otro estilista", true, otroCorte);
        crearCita(otroCorte, otroEstilista, LocalTime.of(11, 0), EstadoCita.CONFIRMADA);
        prepararConsulta();

        mockMvc.perform(disponibilidad(corte, mateo, LUNES))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"horariosLibres":["08:00:00","11:00:00"]}
                        """, JsonCompareMode.LENIENT))
                .andExpect(jsonPath("$.horariosLibres.length()").value(2));
    }

    private MockHttpServletRequestBuilder disponibilidad(Servicio servicio, Usuario estilista, LocalDate fecha) {
        return get(DISPONIBILIDAD, salon.getSlug())
                .param("servicioId", servicio.getId().toString())
                .param("estilistaId", estilista.getId().toString())
                .param("fecha", fecha.toString());
    }

    private static void agregarParametro(MockHttpServletRequestBuilder peticion, String nombre, String valor,
            String parametro, boolean enviarInvalido) {
        if (!nombre.equals(parametro)) {
            peticion.param(nombre, valor);
        } else if (enviarInvalido) {
            peticion.param(nombre, "invalido");
        }
    }

    private void prepararConsulta() {
        entityManager.flush();
        entityManager.clear();
    }

    private Salon crearSalon(String nombre) {
        Salon nuevo = new Salon();
        nuevo.setNombre(nombre);
        nuevo.setSlug("publico-" + UUID.randomUUID());
        nuevo.setCiudad("Bogota");
        nuevo.setDireccion("Calle 10");
        nuevo.setTelefono("5551234");
        nuevo.setPlan(Plan.GRATIS);
        nuevo.setConfiguracion(new ConfiguracionSalon(LocalTime.of(8, 0), LocalTime.NOON, false, 30));
        nuevo.setFechaCreacion(SABADO.atStartOfDay());
        entityManager.persist(nuevo);
        return nuevo;
    }

    private Servicio crearServicio(Salon propietario, String nombre, boolean activo) {
        Servicio servicio = new Servicio();
        servicio.setSalon(propietario);
        servicio.setNombre(nombre);
        servicio.setDescripcion(nombre + " de prueba");
        servicio.setDuracionMinutos(45);
        servicio.setPrecio(35000);
        servicio.setActivo(activo);
        entityManager.persist(servicio);
        return servicio;
    }

    private Usuario crearEstilista(Salon propietario, String nombre, boolean activo, Servicio servicio) {
        Usuario estilista = new Usuario();
        estilista.setSalon(propietario);
        estilista.setNombre(nombre);
        estilista.setEmail(UUID.randomUUID() + "@publico.example");
        estilista.setPassword(PASSWORD_PRUEBA);
        estilista.setRol(Rol.ESTILISTA);
        estilista.setActivo(activo);
        estilista.setServicios(Set.of(servicio));
        entityManager.persist(estilista);
        return estilista;
    }

    private void crearCita(Servicio servicio, Usuario estilista, LocalTime hora, EstadoCita estado) {
        Cliente cliente = new Cliente();
        cliente.setSalon(estilista.getSalon());
        cliente.setNombre("Cliente de prueba");
        cliente.setTelefono(UUID.randomUUID().toString());
        entityManager.persist(cliente);

        Cita cita = new Cita();
        cita.setSalon(estilista.getSalon());
        cita.setEstilista(estilista);
        cita.setServicio(servicio);
        cita.setCliente(cliente);
        cita.setInicio(LUNES.atTime(hora));
        cita.setFin(cita.getInicio().plusMinutes(servicio.getDuracionMinutos()));
        cita.setEstado(estado);
        cita.setOrigen(estado == EstadoCita.PENDIENTE ? OrigenCita.EN_LINEA : OrigenCita.SALON);
        entityManager.persist(cita);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RelojDePrueba {
        @Bean
        @Primary
        Clock relojPublico() {
            ZoneId bogota = ZoneId.of("America/Bogota");
            return Clock.fixed(SABADO.atTime(8, 15).atZone(bogota).toInstant(), bogota);
        }
    }
}
