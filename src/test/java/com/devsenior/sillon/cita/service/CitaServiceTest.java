package com.devsenior.sillon.cita.service;

import com.devsenior.sillon.cita.ReglasAgenda;
import com.devsenior.sillon.cita.dto.CitaResquest;
import com.devsenior.sillon.cita.model.Cita;
import com.devsenior.sillon.cita.model.EstadoCita;
import com.devsenior.sillon.cita.model.OrigenCita;
import com.devsenior.sillon.cita.repository.CitaRepository;
import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.cliente.ClienteRepository;
import com.devsenior.sillon.compartido.AccesoDenegadoException;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.model.ConfiguracionSalon;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.repository.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.model.Rol;
import com.devsenior.sillon.usuario.model.Usuario;
import com.devsenior.sillon.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {
    private static final Long SALON_ID = 10L;
    private static final Long ESTILISTA_ID = 20L;
    private static final Long SERVICIO_ID = 30L;
    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 13, 10, 15);

    @Mock private CitaRepository citaRepository;
    @Mock private SalonRepository salonRepository;
    @Mock private ServicioRepository servicioRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ClienteRepository clienteRepository;

    private CitaService citaService;
    private Salon salon;
    private Servicio servicio;
    private Usuario estilista;
    private Cliente cliente;
    private CitaResquest solicitud;

    @BeforeEach
    void prepararServicio() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-13T15:15:00Z"), ZONA_BOGOTA);
        citaService = new CitaService(citaRepository, salonRepository, servicioRepository,
                usuarioRepository, clienteRepository, new ReglasAgenda(reloj), reloj);

        salon = new Salon();
        salon.setId(SALON_ID);
        salon.setConfiguracion(new ConfiguracionSalon(
                LocalTime.of(8, 0), LocalTime.of(19, 0), false, 30));

        servicio = new Servicio();
        servicio.setId(SERVICIO_ID);
        servicio.setNombre("Corte");
        servicio.setDuracionMinutos(30);
        servicio.setPrecio(35000);
        servicio.setActivo(true);

        estilista = new Usuario();
        estilista.setId(ESTILISTA_ID);
        estilista.setNombre("Ana");
        estilista.setActivo(true);
        estilista.setServicios(Set.of(servicio));

        cliente = new Cliente("Cliente", salon, "+573001112233");
        solicitud = new CitaResquest("Cliente", "+57 300 111 2233", SERVICIO_ID,
                ESTILISTA_ID, AHORA.plusHours(1), "Nota");

    }

    @Test
    void citaValidaSeGuardaConfirmadaConElPrecioHistoricoDelServicio() {
        prepararDatosAntesDeValidarFecha();
        when(citaRepository.findCitasQueOcupanAgenda(eq(ESTILISTA_ID), any(), any(), any()))
                .thenReturn(List.of());
        when(clienteRepository.findBySalonIdAndTelefono(SALON_ID, "+573001112233"))
                .thenReturn(Optional.of(cliente));
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var respuesta = citaService.agendarDesdeSalon(solicitud, SALON_ID);

        assertEquals(EstadoCita.CONFIRMADA, respuesta.estado());
        assertEquals(35000, respuesta.precio());
        verify(citaRepository).save(any(Cita.class));
    }

    @Test
    void citaEnElPasadoNoSeGuarda() {
        prepararDatosAntesDeValidarFecha();
        CitaResquest pasada = new CitaResquest("Cliente", "+57 300 111 2233", SERVICIO_ID,
                ESTILISTA_ID, AHORA.minusMinutes(1), null);

        assertThrows(ReglaNegocioException.class, () -> citaService.agendarDesdeSalon(pasada, SALON_ID));
        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    void citaConCruceDeHorarioNoSeGuarda() {
        LocalDateTime inicioOcupada = solicitud.inicio().minusMinutes(15);
        Cita existente = new Cita(salon, cliente, estilista, servicio, servicio.getPrecio(),
                inicioOcupada, OrigenCita.SALON);
        existente.setEstadoCita(EstadoCita.CONFIRMADA);
        prepararDatosAntesDeValidarFecha();
        when(citaRepository.findCitasQueOcupanAgenda(eq(ESTILISTA_ID), any(), any(), any()))
                .thenReturn(List.of(existente));

        assertThrows(ReglaNegocioException.class, () -> citaService.agendarDesdeSalon(solicitud, SALON_ID));
        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    void estilistaQueNoOfreceElServicioNoPuedeAgendar() {
        prepararDatosAntesDeValidarFecha();
        estilista.setServicios(Set.of());

        assertThrows(ReglaNegocioException.class, () -> citaService.agendarDesdeSalon(solicitud, SALON_ID));
        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    void estilistaQueModificaCitaAjenaRecibeNoEncontrado() {
        Cita cita = citaParaCambioDeEstado();
        cita.getEstilista().setId(99L);
        when(citaRepository.findByIdAndSalonId(1L, SALON_ID)).thenReturn(Optional.of(cita));

        assertThrows(NoEncontradoException.class, () -> citaService.cambiarEstado(
                1L, SALON_ID, EstadoCita.COMPLETADA, ESTILISTA_ID, Rol.ESTILISTA));
        verify(citaRepository, never()).saveAndFlush(any(Cita.class));
    }

    @Test
    void estilistaNoPuedeCancelarUnaCita() {
        when(citaRepository.findByIdAndSalonId(1L, SALON_ID))
                .thenReturn(Optional.of(citaParaCambioDeEstado()));

        assertThrows(AccesoDenegadoException.class, () -> citaService.cambiarEstado(
                1L, SALON_ID, EstadoCita.CANCELADA, ESTILISTA_ID, Rol.ESTILISTA));
        verify(citaRepository, never()).saveAndFlush(any(Cita.class));
    }

    @Test
    void transicionNoPermitidaNoGuarda() {
        Cita cita = citaParaCambioDeEstado();
        cita.setEstadoCita(EstadoCita.COMPLETADA);
        when(citaRepository.findByIdAndSalonId(1L, SALON_ID)).thenReturn(Optional.of(cita));

        assertThrows(ReglaNegocioException.class, () -> citaService.cambiarEstado(
                1L, SALON_ID, EstadoCita.CONFIRMADA, ESTILISTA_ID, Rol.DUENO));
        verify(citaRepository, never()).saveAndFlush(any(Cita.class));
    }

    private Cita citaParaCambioDeEstado() {
        Cita cita = new Cita(salon, cliente, estilista, servicio, servicio.getPrecio(),
                AHORA.plusHours(1), OrigenCita.SALON);
        cita.setEstadoCita(EstadoCita.CONFIRMADA);
        return cita;
    }

    private void prepararDatosAntesDeValidarFecha() {
        when(salonRepository.findById(SALON_ID)).thenReturn(Optional.of(salon));
        when(servicioRepository.findByIdAndSalonId(SERVICIO_ID, SALON_ID)).thenReturn(Optional.of(servicio));
        when(usuarioRepository.findByIdAndSalonIdAndRol(ESTILISTA_ID, SALON_ID, Rol.ESTILISTA))
                .thenReturn(Optional.of(estilista));
    }

}
