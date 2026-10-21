package com.devsenior.sillon.cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.cliente.ClienteService;
import com.devsenior.sillon.compartido.excepcion.AccionNoPermitidaException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CitaServiceTest {
    private final CitaRepository citas = mock(CitaRepository.class);
    private final ServicioRepository servicios = mock(ServicioRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final ClienteService clientes = mock(ClienteService.class);
    private final Clock reloj = Clock.fixed(Instant.parse("2026-10-20T02:00:00Z"), ZoneId.of("America/Bogota"));
    private final CitaService service = new CitaService(citas, servicios, usuarios, clientes,
            new ReglasAgenda(reloj), new CitaMapper(), reloj);
    private final Salon salon = new Salon();
    private final Servicio servicio = new Servicio();
    private final Usuario estilista = new Usuario();
    private final Cliente cliente = new Cliente();
    private final LocalDateTime inicio = LocalDateTime.of(2026, 10, 20, 10, 30);

    @BeforeEach void preparar() {
        salon.setId(1L); salon.setConfiguracion(ConfiguracionSalon.porDefecto());
        servicio.setId(2L); servicio.setNombre("Corte"); servicio.setDuracionMinutos(45);
        servicio.setPrecio(35000); servicio.setActivo(true); servicio.setSalon(salon);
        estilista.setId(3L); estilista.setNombre("Sara"); estilista.setRol(Rol.ESTILISTA);
        estilista.setActivo(true); estilista.setSalon(salon); estilista.setServicios(Set.of(servicio));
        cliente.setNombre("Andrés"); cliente.setTelefono("3102223344");
        when(servicios.findByIdAndSalonId(2L, 1L)).thenReturn(Optional.of(servicio));
        when(usuarios.findParaAgendar(3L, 1L, Rol.ESTILISTA)).thenReturn(Optional.of(estilista));
        when(clientes.buscarOCrear(1L, "Andrés", "3102223344")).thenReturn(cliente);
        when(citas.saveAndFlush(any(Cita.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test void citaValidaCalculaFinPrecioEstadoYOrigen() {
        CitaResponse creada = service.crear(1L, request(inicio));
        assertEquals(inicio.plusMinutes(45), creada.fin());
        assertEquals(35000, creada.precio());
        assertEquals(EstadoCita.CONFIRMADA, creada.estado());
        assertEquals(OrigenCita.SALON, creada.origen());
    }
    @Test void pasadoNoGuardaNiCreaCliente() {
        assertThrows(ReglaNegocioException.class, () -> service.crear(1L, request(inicio.minusDays(2))));
        verify(citas, never()).saveAndFlush(any()); verifyNoInteractions(clientes);
    }
    @Test void cruceNoGuardaNiCreaCliente() {
        when(citas.findAllBySalonIdAndEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
                eq(1L), eq(3L), anyList(), any(), any())).thenReturn(List.of(new Cita()));
        assertThrows(ReglaNegocioException.class, () -> service.crear(1L, request(inicio)));
        verify(citas, never()).saveAndFlush(any()); verifyNoInteractions(clientes);
    }
    @Test void estilistaSinServicioEs409() {
        estilista.setServicios(Set.of());
        assertThrows(ReglaNegocioException.class, () -> service.crear(1L, request(inicio)));
        verify(citas, never()).saveAndFlush(any());
    }
    @Test void citaAjenaAlEstilistaEs404() {
        Cita cita = existente(EstadoCita.CONFIRMADA);
        Usuario otro = new Usuario(); otro.setId(8L); cita.setEstilista(otro);
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.cambiarEstado(new UsuarioPrincipal(estilista), 10L, EstadoCita.COMPLETADA));
        verify(citas, never()).saveAndFlush(any());
    }
    @Test void estilistaNoPuedeCancelarSuCita() {
        existente(EstadoCita.CONFIRMADA);
        assertThrows(AccionNoPermitidaException.class,
                () -> service.cambiarEstado(new UsuarioPrincipal(estilista), 10L, EstadoCita.CANCELADA));
        verify(citas, never()).saveAndFlush(any());
    }
    @Test void transicionInvalidaNoGuarda() {
        existente(EstadoCita.COMPLETADA);
        assertThrows(ReglaNegocioException.class,
                () -> service.cambiarEstado(dueno(), 10L, EstadoCita.CONFIRMADA));
        verify(citas, never()).saveAndFlush(any());
    }
    private CitaRequest request(LocalDateTime hora) { return new CitaRequest("Andrés", "3102223344", 2L, 3L, hora, null); }
    private UsuarioPrincipal dueno() {
        Usuario usuario = new Usuario(); usuario.setId(7L); usuario.setRol(Rol.DUENO); usuario.setSalon(salon);
        return new UsuarioPrincipal(usuario);
    }
    private Cita existente(EstadoCita estado) {
        Cita cita = new Cita(); cita.setId(10L); cita.setEstado(estado); cita.setServicio(servicio);
        cita.setEstilista(estilista); cita.setCliente(cliente); cita.setSalon(salon);
        when(citas.findByIdAndSalonId(10L, 1L)).thenReturn(Optional.of(cita));
        return cita;
    }
}
