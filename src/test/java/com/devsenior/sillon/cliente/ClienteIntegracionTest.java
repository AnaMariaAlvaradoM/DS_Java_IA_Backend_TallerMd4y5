package com.devsenior.sillon.cliente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.seguridad.ServicioJwt;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClienteIntegracionTest {

    @Autowired private ClienteService clienteService;
    @Autowired private ClienteRepository clientes;
    @Autowired private SalonRepository salones;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ServicioJwt jwt;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mockMvc;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void creaAlClienteConTelefonoNormalizado() {
        Long salonId = crearSalon().getId();

        Cliente cliente = clienteService.buscarOCrear(salonId, "Sara", " +57 (300) 123-4567 ");

        assertThat(cliente.getId()).isNotNull();
        assertThat(cliente.getNombre()).isEqualTo("Sara");
        assertThat(cliente.getTelefono()).isEqualTo("+573001234567");
        assertThat(clientes.findBySalonIdAndTelefono(salonId, "+573001234567"))
                .map(Cliente::getId).contains(cliente.getId());
    }

    @Test
    void reutilizaElClienteDelMismoSalonSinCambiarSuNombre() {
        Long salonId = crearSalon().getId();
        Cliente original = clienteService.buscarOCrear(salonId, "Sara", "3001234567");

        Cliente encontrado = clienteService.buscarOCrear(salonId, "Otro nombre", "(300) 123-4567");

        assertThat(encontrado.getId()).isEqualTo(original.getId());
        assertThat(encontrado.getNombre()).isEqualTo("Sara");
        assertThat(clientes.findAllBySalonId(salonId)).hasSize(1);
    }

    @Test
    void admiteElMismoTelefonoEnOtroSalonSinReutilizarElCliente() {
        Long salonA = crearSalon().getId();
        Long salonB = crearSalon().getId();

        Cliente primero = clienteService.buscarOCrear(salonA, "Sara", "3001234567");
        Cliente segundo = clienteService.buscarOCrear(salonB, "Mateo", "3001234567");

        assertThat(segundo.getId()).isNotEqualTo(primero.getId());
        assertThat(segundo.getNombre()).isEqualTo("Mateo");
        assertThat(segundo.getSalon().getId()).isEqualTo(salonB);
    }

    @Test
    void mysqlRechazaElDuplicadoEnElMismoSalonPeroNoEnOtro() {
        Long salonA = crearSalon().getId();
        Long salonB = crearSalon().getId();
        String insert = "INSERT INTO clientes (salon_id, nombre, telefono) VALUES (?, ?, ?)";
        String telefono = "3001234567";

        jdbc.update(insert, salonA, "Sara", telefono);
        jdbc.update(insert, salonB, "Mateo", telefono);

        assertThatThrownBy(() -> jdbc.update(insert, salonA, "Duplicado", telefono))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("uk_clientes_salon_telefono");
    }

    @Test
    void listaSoloLosClientesDelSalonAutenticadoYSoloLosCamposPublicados() throws Exception {
        Salon salon = crearSalon();
        Salon otro = crearSalon();
        String token = tokenPara(salon, Rol.DUENO);
        Cliente cliente = clienteService.buscarOCrear(salon.getId(), "Sara", "300-123-4567");
        clienteService.buscarOCrear(otro.getId(), "Otro cliente", "3011234567");

        mockMvc.perform(get("/api/clientes")
                .header("Authorization", "Bearer " + token)
                .param("salonId", otro.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].length()").value(3))
                .andExpect(jsonPath("$[0].id").value(cliente.getId().intValue()))
                .andExpect(jsonPath("$[0].nombre").value("Sara"))
                .andExpect(jsonPath("$[0].telefono").value("3001234567"));
    }

    @Test
    void estilistaNoPuedeListarClientes() throws Exception {
        String token = tokenPara(crearSalon(), Rol.ESTILISTA);

        mockMvc.perform(get("/api/clientes").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(403));
    }

    @Test
    void solicitudSinAutenticacionDevuelve401() throws Exception {
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void dosTransaccionesConLecturasPreviasReutilizanElMismoCliente() throws Exception {
        Long salonId = crearSalon().getId();
        String telefono = "3001234567";
        var executor = Executors.newFixedThreadPool(2);
        var barrera = new CyclicBarrier(2);
        try {
            var primera = executor.submit(() -> buscarOCrearConcurrente(salonId, telefono, "Sara", barrera));
            var segunda = executor.submit(() -> buscarOCrearConcurrente(salonId, telefono, "Mateo", barrera));

            assertThat(primera.get(20, TimeUnit.SECONDS)).isEqualTo(segunda.get(20, TimeUnit.SECONDS));
            assertThat(clientes.findAllBySalonId(salonId)).hasSize(1);
        } finally {
            executor.shutdownNow();
            if (executor.awaitTermination(25, TimeUnit.SECONDS)) {
                jdbc.update("DELETE FROM clientes WHERE salon_id = ?", salonId);
                salones.deleteById(salonId);
            }
        }
    }

    private Long buscarOCrearConcurrente(Long salonId, String telefono, String nombre, CyclicBarrier barrera) {
        return new TransactionTemplate(transactionManager).execute(estado -> {
            assertThat(clientes.findBySalonIdAndTelefono(salonId, telefono)).isEmpty();
            try {
                barrera.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AssertionError(ex);
            } catch (BrokenBarrierException | TimeoutException ex) {
                throw new AssertionError(ex);
            }
            return clienteService.buscarOCrear(salonId, nombre, telefono).getId();
        });
    }

    private Salon crearSalon() {
        Salon salon = new Salon();
        salon.setNombre("Salon de prueba");
        salon.setSlug("clientes-" + UUID.randomUUID());
        salon.setPlan(Plan.GRATIS);
        salon.setConfiguracion(ConfiguracionSalon.porDefecto());
        salon.setFechaCreacion(LocalDateTime.of(2026, 1, 1, 8, 0));
        return salones.save(salon);
    }

    private String tokenPara(Salon salon, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario de prueba");
        usuario.setEmail(UUID.randomUUID() + "@clientes.example");
        usuario.setPassword(passwordEncoder.encode("solo-para-pruebas"));
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuario.setSalon(salon);
        usuarios.save(usuario);
        return jwt.generarToken(usuario.getEmail(), rol.name(), salon.getId());
    }
}
