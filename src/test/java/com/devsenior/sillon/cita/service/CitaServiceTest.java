package com.devsenior.sillon.cita.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devsenior.sillon.cita.dto.CitaRequest;
import com.devsenior.sillon.cita.dto.CitaResponse;
import com.devsenior.sillon.cita.mapper.CitaMapper;
import com.devsenior.sillon.cita.model.Cita;
import com.devsenior.sillon.cita.model.EstadoCita;
import com.devsenior.sillon.cita.model.OrigenCita;
import com.devsenior.sillon.cita.repository.CitaRepository;
import com.devsenior.sillon.cliente.model.Cliente;
import com.devsenior.sillon.cliente.service.ClienteService;
import com.devsenior.sillon.compartido.excepcion.AccionNoPermitidaException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.model.ConfiguracionSalon;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.service.SalonService;
import com.devsenior.sillon.servicio.model.Servicio;
import com.devsenior.sillon.servicio.service.ServicioService;
import com.devsenior.sillon.usuario.model.Usuario;
import com.devsenior.sillon.usuario.service.EstilistaService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Reloj fijo: lunes 19 de octubre de 2026, 9:10 a. m. Las citas se agendan el martes 20. */
@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    private static final Long SALON_ID = 1L;
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final LocalDate MARTES = LocalDate.of(2026, 10, 20);

    @Mock
    private CitaRepository citaRepository;
    @Mock
    private SalonService salonService;
    @Mock
    private ServicioService servicioService;
    @Mock
    private EstilistaService estilistaService;
    @Mock
    private ClienteService clienteService;

    private CitaService citaService;
    private Salon salon;
    private Servicio corte;
    private Servicio barba;
    private Usuario mateo;
    private Usuario sara;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(HOY.atTime(9, 10).atZone(BOGOTA).toInstant(), BOGOTA);
        citaService = new CitaService(citaRepository, salonService, servicioService, estilistaService,
                clienteService, new ReglasAgenda(reloj), new CitaMapper());

        salon = new Salon();
        salon.setId(SALON_ID);
        salon.setConfiguracion(ConfiguracionSalon.porDefecto());
        corte = servicio(10L, "Corte clásico", 45, 35000);
        barba = servicio(11L, "Arreglo de barba", 30, 20000);
        mateo = estilista(3L, "Mateo Gil", corte, barba);
        sara = estilista(4L, "Sara Mejía", corte);
    }

    @Test
    void crearDesdeSalon_citaValida_quedaConfirmadaConPrecioYFinCalculado() {
        prepararAgendaLibre(corte, sara);

        CitaResponse cita = citaService.crearDesdeSalon(SALON_ID, request(corte, sara, MARTES.atTime(10, 30)));

        assertEquals(EstadoCita.CONFIRMADA, cita.estado());
        assertEquals(OrigenCita.SALON, cita.origen());
        assertEquals(35000, cita.precio());
        assertEquals(MARTES.atTime(11, 15), cita.fin());
    }

    @Test
    void reservarEnLinea_citaValida_quedaPendienteYEnLinea() {
        prepararAgendaLibre(corte, mateo);

        CitaResponse cita = citaService.reservarEnLinea(SALON_ID, request(corte, mateo, MARTES.atTime(10, 0)));

        assertEquals(EstadoCita.PENDIENTE, cita.estado());
        assertEquals(OrigenCita.EN_LINEA, cita.origen());
        assertEquals(MARTES.atTime(10, 45), cita.fin());
    }

    @Test
    void crearDesdeSalon_enElPasado_lanza409YNoGuarda() {
        when(servicioService.obtenerEntidad(SALON_ID, corte.getId())).thenReturn(corte);
        when(estilistaService.obtenerActivo(SALON_ID, mateo.getId())).thenReturn(mateo);

        assertThrows(ReglaNegocioException.class,
                () -> citaService.crearDesdeSalon(SALON_ID, request(corte, mateo, HOY.atTime(8, 0))));
        verify(citaRepository, never()).save(any());
    }

    @Test
    void crearDesdeSalon_conCruce_lanza409YNoGuardaNiCreaCliente() {
        when(servicioService.obtenerEntidad(SALON_ID, barba.getId())).thenReturn(barba);
        when(estilistaService.obtenerActivo(SALON_ID, mateo.getId())).thenReturn(mateo);
        when(salonService.obtenerEntidad(SALON_ID)).thenReturn(salon);
        when(citaRepository.existsByEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
                anyLong(), anyCollection(), any(), any())).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> citaService.crearDesdeSalon(SALON_ID, request(barba, mateo, MARTES.atTime(10, 30))));
        verify(citaRepository, never()).save(any());
        verify(clienteService, never()).buscarOCrear(any(), anyString(), anyString());
    }

    @Test
    void crearDesdeSalon_estilistaQueNoOfreceElServicio_lanza409() {
        when(servicioService.obtenerEntidad(SALON_ID, barba.getId())).thenReturn(barba);
        when(estilistaService.obtenerActivo(SALON_ID, sara.getId())).thenReturn(sara);

        assertThrows(ReglaNegocioException.class,
                () -> citaService.crearDesdeSalon(SALON_ID, request(barba, sara, MARTES.atTime(15, 0))));
        verify(citaRepository, never()).save(any());
    }

    @Test
    void crearDesdeSalon_servicioInactivo_lanza404() {
        corte.setActivo(false);
        when(servicioService.obtenerEntidad(SALON_ID, corte.getId())).thenReturn(corte);

        assertThrows(RecursoNoEncontradoException.class,
                () -> citaService.crearDesdeSalon(SALON_ID, request(corte, sara, MARTES.atTime(10, 0))));
    }

    @Test
    void cambiarEstado_estilistaTocaCitaAjena_lanza404() {
        when(citaRepository.findByIdAndSalonId(50L, SALON_ID)).thenReturn(Optional.of(cita(sara, EstadoCita.CONFIRMADA)));

        assertThrows(RecursoNoEncontradoException.class,
                () -> citaService.cambiarEstado(SALON_ID, mateo.getId(), true, 50L, EstadoCita.COMPLETADA));
        verify(citaRepository, never()).saveAndFlush(any());
    }

    @Test
    void cambiarEstado_estilistaIntentaCancelar_lanza403() {
        when(citaRepository.findByIdAndSalonId(50L, SALON_ID)).thenReturn(Optional.of(cita(mateo, EstadoCita.CONFIRMADA)));

        assertThrows(AccionNoPermitidaException.class,
                () -> citaService.cambiarEstado(SALON_ID, mateo.getId(), true, 50L, EstadoCita.CANCELADA));
        verify(citaRepository, never()).saveAndFlush(any());
    }

    @Test
    void cambiarEstado_transicionNoPermitida_lanza409YNoGuarda() {
        when(citaRepository.findByIdAndSalonId(50L, SALON_ID)).thenReturn(Optional.of(cita(mateo, EstadoCita.CONFIRMADA)));

        assertThrows(ReglaNegocioException.class,
                () -> citaService.cambiarEstado(SALON_ID, 99L, false, 50L, EstadoCita.PENDIENTE));
        verify(citaRepository, never()).saveAndFlush(any());
    }

    @Test
    void cambiarEstado_estilistaMarcaSuCitaCompletada_laGuarda() {
        when(citaRepository.findByIdAndSalonId(50L, SALON_ID)).thenReturn(Optional.of(cita(mateo, EstadoCita.CONFIRMADA)));
        when(citaRepository.saveAndFlush(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

        CitaResponse respuesta = citaService.cambiarEstado(SALON_ID, mateo.getId(), true, 50L, EstadoCita.COMPLETADA);

        assertEquals(EstadoCita.COMPLETADA, respuesta.estado());
    }

    @Test
    void cambiarEstado_citaDeOtroSalon_lanza404() {
        when(citaRepository.findByIdAndSalonId(50L, 2L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> citaService.cambiarEstado(2L, 7L, false, 50L, EstadoCita.CANCELADA));
    }

    @Test
    void disponibilidad_despuesDeLaReservaDeValentina_noOfreceLosCrucesComoTexto() {
        when(servicioService.obtenerEntidad(SALON_ID, corte.getId())).thenReturn(corte);
        when(estilistaService.obtenerActivo(SALON_ID, mateo.getId())).thenReturn(mateo);
        when(salonService.obtenerEntidad(SALON_ID)).thenReturn(salon);
        Cita valentina = cita(mateo, EstadoCita.PENDIENTE);
        valentina.setInicio(MARTES.atTime(10, 0));
        valentina.setFin(MARTES.atTime(10, 45));
        when(citaRepository.findByEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
                anyLong(), anyCollection(), any(), any())).thenReturn(java.util.List.of(valentina));

        var libres = citaService.disponibilidad(SALON_ID, corte.getId(), mateo.getId(), MARTES).horariosLibres();

        assertEquals(false, libres.contains("09:30:00"));
        assertEquals(false, libres.contains("10:00:00"));
        assertEquals(false, libres.contains("10:30:00"));
        assertEquals(true, libres.contains("11:00:00"));
    }

    @Test
    void disponibilidad_estilistaQueNoOfreceElServicio_lanza409() {
        when(servicioService.obtenerEntidad(SALON_ID, barba.getId())).thenReturn(barba);
        when(estilistaService.obtenerActivo(SALON_ID, sara.getId())).thenReturn(sara);

        assertThrows(ReglaNegocioException.class,
                () -> citaService.disponibilidad(SALON_ID, barba.getId(), sara.getId(), MARTES));
    }

    @Test
    void datosRecordatorio_citaConfirmada_escribeUnaLineaPorDatoEnEspanolDeColombia() {
        salon.setNombre("Barbería La Navaja");
        salon.setDireccion("Calle 10 # 43-20");
        salon.setCiudad("Medellín");
        Cita cita = cita(sara, EstadoCita.CONFIRMADA);
        cita.setSalon(salon);
        cita.setPrecio(35000);
        when(citaRepository.findByIdAndSalonId(50L, SALON_ID)).thenReturn(Optional.of(cita));

        String datos = citaService.datosRecordatorio(SALON_ID, 50L).datos();

        assertEquals(String.join("\n",
                "- Servicio: Corte clásico (45 minutos)",
                "- Estilista: Sara Mejía",
                "- Fecha: martes 20 de octubre",
                "- Hora: 10:30 a. m.",
                "- Valor: $35.000",
                "- Dirección: Calle 10 # 43-20, Medellín"), datos);
    }

    @Test
    void datosRecordatorio_citaCompletada_lanza409() {
        when(citaRepository.findByIdAndSalonId(50L, SALON_ID)).thenReturn(Optional.of(cita(mateo, EstadoCita.COMPLETADA)));

        assertThrows(ReglaNegocioException.class, () -> citaService.datosRecordatorio(SALON_ID, 50L));
    }

    private void prepararAgendaLibre(Servicio servicio, Usuario estilista) {
        when(servicioService.obtenerEntidad(SALON_ID, servicio.getId())).thenReturn(servicio);
        when(estilistaService.obtenerActivo(SALON_ID, estilista.getId())).thenReturn(estilista);
        when(salonService.obtenerEntidad(SALON_ID)).thenReturn(salon);
        when(citaRepository.existsByEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
                anyLong(), anyCollection(), any(), any())).thenReturn(false);
        Cliente cliente = new Cliente();
        cliente.setNombre("Andrés Gómez");
        cliente.setTelefono("3102223344");
        when(clienteService.buscarOCrear(any(), anyString(), anyString())).thenReturn(cliente);
        when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CitaRequest request(Servicio servicio, Usuario estilista, LocalDateTime inicio) {
        return new CitaRequest("Andrés Gómez", "310 222 3344", servicio.getId(), estilista.getId(), inicio, null);
    }

    private Cita cita(Usuario estilista, EstadoCita estado) {
        Cliente cliente = new Cliente();
        cliente.setNombre("Andrés Gómez");
        cliente.setTelefono("3102223344");
        Cita cita = new Cita();
        cita.setId(50L);
        cita.setEstilista(estilista);
        cita.setServicio(corte);
        cita.setCliente(cliente);
        cita.setEstado(estado);
        cita.setOrigen(OrigenCita.SALON);
        cita.setInicio(MARTES.atTime(10, 30));
        cita.setFin(MARTES.atTime(11, 15));
        return cita;
    }

    private Servicio servicio(Long id, String nombre, int duracion, int precio) {
        Servicio servicio = new Servicio();
        servicio.setId(id);
        servicio.setNombre(nombre);
        servicio.setDuracionMinutos(duracion);
        servicio.setPrecio(precio);
        servicio.setActivo(true);
        return servicio;
    }

    private Usuario estilista(Long id, String nombre, Servicio... servicios) {
        Usuario estilista = new Usuario();
        estilista.setId(id);
        estilista.setNombre(nombre);
        estilista.setActivo(true);
        estilista.setServicios(Set.of(servicios));
        return estilista;
    }
}
