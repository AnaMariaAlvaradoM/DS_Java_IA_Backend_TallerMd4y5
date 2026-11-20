package com.devsenior.sillon.cita;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class CitaPersistenciaTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDateTime CREACION = LocalDateTime.of(2026, 11, 19, 9, 0);

    @Autowired private CitaRepository citas;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private Clock clock;

    @BeforeEach
    void fijarReloj() {
        given(clock.getZone()).willReturn(BOGOTA);
        given(clock.instant()).willReturn(CREACION.atZone(BOGOTA).toInstant());
    }

    @Test
    void recuperaLasCuatroRelacionesYGuardaLosEnumsComoTexto() {
        Cita original = citaNueva(EstadoCita.PENDIENTE, OrigenCita.EN_LINEA);
        Long otraSala = crearSalon().getId();
        original.setNota("Primera visita");
        Long id = citas.saveAndFlush(original).getId();
        entityManager.clear();

        assertThat(citas.findByIdAndSalonId(id, otraSala)).isEmpty();
        Cita recuperada = citas.findByIdAndSalonId(id, original.getSalon().getId()).orElseThrow();
        assertThat(recuperada.getSalon().getNombre()).isEqualTo("Salon de prueba");
        assertThat(recuperada.getCliente().getNombre()).isEqualTo("Sara");
        assertThat(recuperada.getEstilista().getNombre()).isEqualTo("Mateo");
        assertThat(recuperada.getServicio().getNombre()).isEqualTo("Corte");
        assertThat(recuperada.getInicio()).isEqualTo(original.getInicio());
        assertThat(recuperada.getFin()).isEqualTo(original.getFin());
        assertThat(recuperada.getNota()).isEqualTo("Primera visita");
        assertThat(recuperada.getEstado()).isEqualTo(EstadoCita.PENDIENTE);
        assertThat(recuperada.getOrigen()).isEqualTo(OrigenCita.EN_LINEA);
        assertThat(jdbc.queryForMap("select estado, origen from citas where id = ?", id))
                .containsEntry("estado", "PENDIENTE")
                .containsEntry("origen", "EN_LINEA");
    }

    @Test
    void asignaFechasConElRelojDelProyectoYActualizaAlHacerFlushSinCambiarElPrecio() {
        Cita cita = citas.saveAndFlush(citaNueva(EstadoCita.CONFIRMADA, OrigenCita.SALON));
        assertThat(cita.getFechaCreacion()).isEqualTo(CREACION);
        assertThat(cita.getFechaActualizacion()).isEqualTo(CREACION);
        assertThat(cita.getPrecio()).isEqualTo(35000);

        LocalDateTime actualizacion = CREACION.plusDays(1);
        given(clock.instant()).willReturn(actualizacion.atZone(BOGOTA).toInstant());
        cita.getServicio().setPrecio(50000);
        cita.setEstado(EstadoCita.COMPLETADA);
        cita = citas.saveAndFlush(cita);
        assertThat(cita.getFechaActualizacion()).isEqualTo(actualizacion);
        entityManager.clear();

        Cita recuperada = citas.findByIdAndSalonId(cita.getId(), cita.getSalon().getId()).orElseThrow();
        assertThat(recuperada.getFechaCreacion()).isEqualTo(CREACION);
        assertThat(recuperada.getFechaActualizacion()).isEqualTo(actualizacion);
        assertThat(recuperada.getPrecio()).isEqualTo(35000);
        assertThat(recuperada.getServicio().getPrecio()).isEqualTo(50000);
        assertThat(recuperada.getNota()).isNull();
    }

    private Salon crearSalon() {
        Salon salon = new Salon();
        salon.setNombre("Salon de prueba");
        salon.setSlug("cita-" + UUID.randomUUID());
        salon.setPlan(Plan.GRATIS);
        salon.setFechaCreacion(CREACION);
        entityManager.persist(salon);
        return salon;
    }

    private Cita citaNueva(EstadoCita estado, OrigenCita origen) {
        Salon salon = crearSalon();

        Cliente cliente = new Cliente();
        cliente.setSalon(salon);
        cliente.setNombre("Sara");
        cliente.setTelefono("3001234567");
        entityManager.persist(cliente);

        Usuario estilista = new Usuario();
        estilista.setSalon(salon);
        estilista.setNombre("Mateo");
        estilista.setEmail(UUID.randomUUID() + "@citas.example");
        estilista.setPassword("valor-no-usado-para-autenticacion");
        estilista.setRol(Rol.ESTILISTA);
        estilista.setActivo(true);
        entityManager.persist(estilista);

        Servicio servicio = new Servicio();
        servicio.setSalon(salon);
        servicio.setNombre("Corte");
        servicio.setPrecio(35000);
        servicio.setDuracionMinutos(45);
        servicio.setActivo(true);
        entityManager.persist(servicio);

        Cita cita = new Cita();
        cita.setSalon(salon);
        cita.setCliente(cliente);
        cita.setEstilista(estilista);
        cita.setServicio(servicio);
        cita.setInicio(CREACION.plusHours(1));
        cita.setFin(cita.getInicio().plusMinutes(45));
        cita.setEstado(estado);
        cita.setOrigen(origen);
        return cita;
    }
}
