package com.devsenior.sillon.cita;

import com.devsenior.sillon.cita.dto.CambiarEstadoCitaRequest;
import com.devsenior.sillon.cita.dto.CrearCitaRequest;
import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.cliente.ClienteService;
import com.devsenior.sillon.compartido.excepcion.AccionNoPermitidaException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.ia.MotorIaService;
import com.devsenior.sillon.ia.PromptService;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendaTest {

    @Mock
    private CitaRepository citaRepository;
    @Mock
    private SalonRepository salonRepository;
    @Mock
    private ServicioRepository servicioRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ClienteService clienteService;
    @Mock
    private MotorIaService motorIaService;
    @Mock
    private PromptService promptService;

    private ReglasAgenda reglasAgenda;
    private Clock clockFijo;
    private CitaService citaService;
    private ConfiguracionSalon configDefecto;

    @BeforeEach
    void setUp() {
        reglasAgenda = new ReglasAgenda();
        // Viernes 2026-11-20 08:00 America/Bogota
        clockFijo = Clock.fixed(
                LocalDateTime.of(2026, 11, 20, 8, 0).atZone(ZoneId.of("America/Bogota")).toInstant(),
                ZoneId.of("America/Bogota")
        );
        citaService = new CitaService(
                citaRepository,
                salonRepository,
                servicioRepository,
                usuarioRepository,
                clienteService,
                reglasAgenda,
                motorIaService,
                promptService,
                clockFijo
        );
        configDefecto = ConfiguracionSalon.porDefecto();
    }

    @Test
    void validarHorarioDentroDeRangoNoLanzaExcepcion() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 21, 10, 0);
        LocalDateTime fin = inicio.plusMinutes(45);
        assertDoesNotThrow(() -> reglasAgenda.validarHorario(inicio, fin, configDefecto));
    }

    @Test
    void rechazaCitaAntesDeHoraAperturaCon409() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 21, 7, 30);
        LocalDateTime fin = inicio.plusMinutes(45);
        assertThrows(ReglaNegocioException.class, () -> reglasAgenda.validarHorario(inicio, fin, configDefecto));
    }

    @Test
    void rechazaCitaQueTerminaDespuesDelCierreCon409() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 21, 18, 45);
        LocalDateTime fin = inicio.plusMinutes(45); // 19:30 > 19:00
        assertThrows(ReglaNegocioException.class, () -> reglasAgenda.validarHorario(inicio, fin, configDefecto));
    }

    @Test
    void rechazaCitaEnDomingoCuandoNoAbreDomingos() {
        LocalDateTime domingo = LocalDateTime.of(2026, 11, 22, 10, 0); // Domingo
        LocalDateTime fin = domingo.plusMinutes(30);
        assertThrows(ReglaNegocioException.class, () -> reglasAgenda.validarHorario(domingo, fin, configDefecto));
    }

    @Test
    void aceptaCitaEnDomingoCuandoSalonAbreDomingos() {
        ConfiguracionSalon configDomingo = new ConfiguracionSalon(LocalTime.of(8, 0), LocalTime.of(19, 0), true, 30);
        LocalDateTime domingo = LocalDateTime.of(2026, 11, 22, 10, 0);
        LocalDateTime fin = domingo.plusMinutes(30);
        assertDoesNotThrow(() -> reglasAgenda.validarHorario(domingo, fin, configDomingo));
    }

    @Test
    void rechazaCitaEnElPasadoCon409() {
        Salon salon = crearSalonEjemplo();
        Servicio servicio = crearServicioEjemplo(salon);
        Usuario estilista = crearEstilistaEjemplo(salon, servicio);

        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
        when(servicioRepository.findByIdAndSalonId(10L, 1L)).thenReturn(Optional.of(servicio));
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(estilista));

        LocalDateTime pasado = LocalDateTime.of(2026, 11, 19, 10, 0);
        CrearCitaRequest request = new CrearCitaRequest("Andres", "3001234567", 10L, 20L, pasado, null);

        assertThrows(ReglaNegocioException.class, () -> citaService.agendarCitaSalon(1L, request));
    }

    @Test
    void detectaCruceDeCitasSolapadas() {
        LocalDateTime i1 = LocalDateTime.of(2026, 11, 21, 10, 0);
        LocalDateTime f1 = LocalDateTime.of(2026, 11, 21, 10, 45);
        LocalDateTime i2 = LocalDateTime.of(2026, 11, 21, 10, 30);
        LocalDateTime f2 = LocalDateTime.of(2026, 11, 21, 11, 0);

        assertTrue(reglasAgenda.seCruzan(i1, f1, i2, f2));
    }

    @Test
    void citasContiguasNoSeCruzan() {
        LocalDateTime i1 = LocalDateTime.of(2026, 11, 21, 10, 0);
        LocalDateTime f1 = LocalDateTime.of(2026, 11, 21, 10, 45);
        LocalDateTime i2 = LocalDateTime.of(2026, 11, 21, 10, 45);
        LocalDateTime f2 = LocalDateTime.of(2026, 11, 21, 11, 15);

        assertFalse(reglasAgenda.seCruzan(i1, f1, i2, f2));
    }

    @Test
    void calculaHorariosLibresDescartandoCrucesYHorasPasadas() {
        LocalDate fecha = LocalDate.of(2026, 11, 21);
        Cita ocupada = new Cita();
        ocupada.setInicio(LocalDateTime.of(2026, 11, 21, 10, 0));
        ocupada.setFin(LocalDateTime.of(2026, 11, 21, 10, 45));

        LocalDateTime ahora = LocalDateTime.of(2026, 11, 21, 8, 30);
        List<LocalTime> libres = reglasAgenda.calcularHorariosLibres(fecha, 45, configDefecto, List.of(ocupada), ahora);

        assertFalse(libres.contains(LocalTime.of(8, 0)));  // pasada
        assertFalse(libres.contains(LocalTime.of(8, 30))); // pasada/actual
        assertTrue(libres.contains(LocalTime.of(9, 0)));   // 9:00-9:45 libre
        assertFalse(libres.contains(LocalTime.of(9, 30))); // 9:30-10:15 se cruza con 10:00
        assertFalse(libres.contains(LocalTime.of(10, 0))); // 10:00-10:45 ocupada
        assertFalse(libres.contains(LocalTime.of(10, 30))); // 10:30-11:15 se cruza
        assertTrue(libres.contains(LocalTime.of(11, 0)));  // 11:00-11:45 libre
    }

    @Test
    void maquinaEstadosPendienteSoloTransicionaAConfirmadaOCancelada() {
        assertTrue(EstadoCita.PENDIENTE.puedeTransicionarA(EstadoCita.CONFIRMADA));
        assertTrue(EstadoCita.PENDIENTE.puedeTransicionarA(EstadoCita.CANCELADA));
        assertFalse(EstadoCita.PENDIENTE.puedeTransicionarA(EstadoCita.COMPLETADA));
        assertFalse(EstadoCita.PENDIENTE.puedeTransicionarA(EstadoCita.NO_ASISTIO));
    }

    @Test
    void maquinaEstadosConfirmadaTransicionaACompletadaCanceladaONoAsistio() {
        assertTrue(EstadoCita.CONFIRMADA.puedeTransicionarA(EstadoCita.COMPLETADA));
        assertTrue(EstadoCita.CONFIRMADA.puedeTransicionarA(EstadoCita.CANCELADA));
        assertTrue(EstadoCita.CONFIRMADA.puedeTransicionarA(EstadoCita.NO_ASISTIO));
        assertFalse(EstadoCita.CONFIRMADA.puedeTransicionarA(EstadoCita.PENDIENTE));
    }

    @Test
    void maquinaEstadosFinalesNoPermitenTransiciones() {
        for (EstadoCita destino : EstadoCita.values()) {
            assertFalse(EstadoCita.COMPLETADA.puedeTransicionarA(destino));
            assertFalse(EstadoCita.CANCELADA.puedeTransicionarA(destino));
            assertFalse(EstadoCita.NO_ASISTIO.puedeTransicionarA(destino));
        }
    }

    @Test
    void estilistaNoPuedeModificarCitaDeOtroEstilistaLanza404() {
        Salon salon = crearSalonEjemplo();
        Servicio servicio = crearServicioEjemplo(salon);
        Usuario sara = crearEstilistaEjemplo(salon, servicio);
        sara.setId(21L);

        Cita citaDeSara = crearCitaEjemplo(salon, servicio, sara, EstadoCita.CONFIRMADA);
        when(citaRepository.findByIdAndSalonId(100L, 1L)).thenReturn(Optional.of(citaDeSara));

        UsuarioPrincipal mateoPrincipal = new UsuarioPrincipal(20L, 1L, "mateo@salon.com", "hash", Rol.ESTILISTA, true);
        CambiarEstadoCitaRequest req = new CambiarEstadoCitaRequest(EstadoCita.COMPLETADA);

        assertThrows(RecursoNoEncontradoException.class, () -> citaService.cambiarEstadoCita(mateoPrincipal, 100L, req));
    }

    @Test
    void estilistaNoPuedeCancelarSuPropiaCitaLanza403() {
        Salon salon = crearSalonEjemplo();
        Servicio servicio = crearServicioEjemplo(salon);
        Usuario mateo = crearEstilistaEjemplo(salon, servicio);

        Cita citaDeMateo = crearCitaEjemplo(salon, servicio, mateo, EstadoCita.CONFIRMADA);
        when(citaRepository.findByIdAndSalonId(100L, 1L)).thenReturn(Optional.of(citaDeMateo));

        UsuarioPrincipal mateoPrincipal = new UsuarioPrincipal(20L, 1L, "mateo@salon.com", "hash", Rol.ESTILISTA, true);
        CambiarEstadoCitaRequest req = new CambiarEstadoCitaRequest(EstadoCita.CANCELADA);

        assertThrows(AccionNoPermitidaException.class, () -> citaService.cambiarEstadoCita(mateoPrincipal, 100L, req));
    }

    private Salon crearSalonEjemplo() {
        Salon salon = new Salon();
        salon.setId(1L);
        salon.setNombre("Barberia La Navaja");
        salon.setSlug("barberia-la-navaja");
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(10);
        salon.setConfiguracion(configDefecto);
        return salon;
    }

    private Servicio crearServicioEjemplo(Salon salon) {
        Servicio s = new Servicio();
        s.setId(10L);
        s.setNombre("Corte clasico");
        s.setDuracionMinutos(45);
        s.setPrecio(35000);
        s.setActivo(true);
        s.setSalon(salon);
        return s;
    }

    private Usuario crearEstilistaEjemplo(Salon salon, Servicio servicio) {
        Usuario u = new Usuario();
        u.setId(20L);
        u.setNombre("Mateo Gil");
        u.setEmail("mateo@salon.com");
        u.setRol(Rol.ESTILISTA);
        u.setActivo(true);
        u.setSalon(salon);
        u.setServicios(List.of(servicio));
        return u;
    }

    private Cita crearCitaEjemplo(Salon salon, Servicio servicio, Usuario estilista, EstadoCita estado) {
        Cliente cliente = new Cliente(1L, "Valentina", "3001112233", salon);
        Cita cita = new Cita();
        cita.setId(100L);
        cita.setSalon(salon);
        cita.setServicio(servicio);
        cita.setEstilista(estilista);
        cita.setCliente(cliente);
        cita.setInicio(LocalDateTime.of(2026, 11, 21, 10, 0));
        cita.setFin(LocalDateTime.of(2026, 11, 21, 10, 45));
        cita.setPrecio(35000);
        cita.setEstado(estado);
        cita.setOrigen(OrigenCita.SALON);
        return cita;
    }
}
