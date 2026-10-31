package com.devsenior.sillon.cita;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.cliente.ClienteService;
import com.devsenior.sillon.compartido.AccionNoPermitidaException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    // martes 2026-03-10, 10:00 a.m. hora Bogota
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-10T15:00:00Z"), ZONA);
    private static final Long SALON_ID = 1L;

    @Mock
    private CitaRepository citaRepository;
    @Mock
    private ServicioRepository servicioRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private SalonRepository salonRepository;
    @Mock
    private ClienteService clienteService;

    private CitaService citaService;

    private Salon salon;
    private Servicio servicio;
    private Usuario estilista;

    @BeforeEach
    void setUp() {
        ReglasAgenda reglasAgenda = new ReglasAgenda(CLOCK);
        citaService = new CitaService(citaRepository, servicioRepository, usuarioRepository, salonRepository,
                clienteService, reglasAgenda, CLOCK);

        salon = new Salon();
        salon.setId(SALON_ID);
        salon.setConfiguracion(ConfiguracionSalon.porDefecto());

        servicio = new Servicio();
        servicio.setId(10L);
        servicio.setNombre("Corte");
        servicio.setDuracionMinutos(30);
        servicio.setPrecio(50000L);
        servicio.setActivo(true);

        estilista = new Usuario();
        estilista.setId(20L);
        estilista.setNombre("Ana");
        estilista.setActivo(true);
        estilista.setServicios(new HashSet<>(List.of(servicio)));
    }

    private CitaRequest requestValido(LocalDateTime inicio) {
        return new CitaRequest("Cliente Uno", "3001234567", servicio.getId(), estilista.getId(), inicio, null);
    }

    @Test
    void agendarCitaValidaQuedaConfirmadaConElPrecioDelServicio() {
        LocalDateTime inicio = LocalDateTime.of(2026, 3, 11, 10, 0); // miercoles, dentro de horario, futuro
        when(servicioRepository.findByIdAndSalonId(servicio.getId(), SALON_ID)).thenReturn(Optional.of(servicio));
        when(usuarioRepository.findByIdAndSalonIdAndRol(estilista.getId(), SALON_ID, Rol.ESTILISTA))
                .thenReturn(Optional.of(estilista));
        when(salonRepository.findById(SALON_ID)).thenReturn(Optional.of(salon));
        when(citaRepository.findBySalonIdAndEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(eq(SALON_ID),
                eq(estilista.getId()), anyList(), any(), any())).thenReturn(List.of());

        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente Uno");
        cliente.setTelefono("3001234567");
        when(clienteService.buscarOCrear(SALON_ID, "Cliente Uno", "3001234567")).thenReturn(cliente);

        when(citaRepository.save(any(Cita.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        CitaRespuesta respuesta = citaService.agendar(SALON_ID, requestValido(inicio), EstadoCita.CONFIRMADA,
                OrigenCita.SALON);

        assertThat(respuesta.estado()).isEqualTo(EstadoCita.CONFIRMADA);
        assertThat(respuesta.precio()).isEqualTo(50000L);
    }

    @Test
    void noGuardaUnaCitaEnElPasado() {
        when(servicioRepository.findByIdAndSalonId(servicio.getId(), SALON_ID)).thenReturn(Optional.of(servicio));
        when(usuarioRepository.findByIdAndSalonIdAndRol(estilista.getId(), SALON_ID, Rol.ESTILISTA))
                .thenReturn(Optional.of(estilista));

        LocalDateTime inicioPasado = LocalDateTime.of(2026, 3, 9, 10, 0); // antes del Clock fijo

        assertThatThrownBy(
                () -> citaService.agendar(SALON_ID, requestValido(inicioPasado), EstadoCita.CONFIRMADA,
                        OrigenCita.SALON))
                .isInstanceOf(ReglaNegocioException.class);

        verify(citaRepository, never()).save(any());
    }

    @Test
    void noGuardaUnaCitaConCruceDeHorario() {
        LocalDateTime inicio = LocalDateTime.of(2026, 3, 11, 10, 0);
        when(servicioRepository.findByIdAndSalonId(servicio.getId(), SALON_ID)).thenReturn(Optional.of(servicio));
        when(usuarioRepository.findByIdAndSalonIdAndRol(estilista.getId(), SALON_ID, Rol.ESTILISTA))
                .thenReturn(Optional.of(estilista));
        when(salonRepository.findById(SALON_ID)).thenReturn(Optional.of(salon));
        when(citaRepository.findBySalonIdAndEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(eq(SALON_ID),
                eq(estilista.getId()), anyList(), any(), any())).thenReturn(List.of(new Cita()));

        assertThatThrownBy(
                () -> citaService.agendar(SALON_ID, requestValido(inicio), EstadoCita.CONFIRMADA, OrigenCita.SALON))
                .isInstanceOf(ReglaNegocioException.class);

        verify(citaRepository, never()).save(any());
    }

    @Test
    void elEstilistaQueNoOfreceElServicioNoPuedeSerAgendado() {
        estilista.setServicios(new HashSet<>()); // no ofrece el servicio
        LocalDateTime inicio = LocalDateTime.of(2026, 3, 11, 10, 0);
        when(servicioRepository.findByIdAndSalonId(servicio.getId(), SALON_ID)).thenReturn(Optional.of(servicio));
        when(usuarioRepository.findByIdAndSalonIdAndRol(estilista.getId(), SALON_ID, Rol.ESTILISTA))
                .thenReturn(Optional.of(estilista));

        assertThatThrownBy(
                () -> citaService.agendar(SALON_ID, requestValido(inicio), EstadoCita.CONFIRMADA, OrigenCita.SALON))
                .isInstanceOf(ReglaNegocioException.class);

        verify(citaRepository, never()).save(any());
    }

    @Test
    void unEstilistaQueIntentaCambiarLaCitaDeOtroRecibeNoEncontrada() {
        Cita cita = new Cita();
        cita.setEstado(EstadoCita.CONFIRMADA);
        cita.setEstilista(estilista); // id 20

        when(citaRepository.findByIdAndSalonId(99L, SALON_ID)).thenReturn(Optional.of(cita));

        assertThatThrownBy(() -> citaService.cambiarEstado(SALON_ID, 99L, 999L, Rol.ESTILISTA,
                EstadoCita.COMPLETADA))
                .isInstanceOf(com.devsenior.sillon.compartido.RecursoNoEncontradoException.class);
    }

    @Test
    void unEstilistaQueIntentaCancelarSuPropiaCitaNoPuede() {
        Cita cita = new Cita();
        cita.setEstado(EstadoCita.CONFIRMADA);
        cita.setEstilista(estilista); // id 20

        when(citaRepository.findByIdAndSalonId(99L, SALON_ID)).thenReturn(Optional.of(cita));

        assertThatThrownBy(() -> citaService.cambiarEstado(SALON_ID, 99L, estilista.getId(), Rol.ESTILISTA,
                EstadoCita.CANCELADA))
                .isInstanceOf(AccionNoPermitidaException.class);
    }

    @Test
    void unaTransicionNoPermitidaNoSeGuarda() {
        Cita cita = new Cita();
        cita.setEstado(EstadoCita.COMPLETADA);
        cita.setEstilista(estilista);

        when(citaRepository.findByIdAndSalonId(99L, SALON_ID)).thenReturn(Optional.of(cita));

        assertThatThrownBy(() -> citaService.cambiarEstado(SALON_ID, 99L, 1L, Rol.DUENO, EstadoCita.CONFIRMADA))
                .isInstanceOf(ReglaNegocioException.class);

        verify(citaRepository, never()).saveAndFlush(any());
    }
}
