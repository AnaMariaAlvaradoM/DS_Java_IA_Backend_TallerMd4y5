
package com.devsenior.sillon.cita;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.cliente.ClienteService;
import com.devsenior.sillon.compartido.AccionNoPermitidaException;
import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CitaServiceTest {

    private CitaRepository citas;
    private SalonRepository salones;
    private ServicioRepository servicios;
    private UsuarioRepository usuarios;
    private ClienteService clientes;

    private CitaService servicioCitas;

    private Salon salon;
    private Servicio corte;
    private Usuario estilista;

    @BeforeEach
    void configurar() {

        citas = mock(CitaRepository.class);
        salones = mock(SalonRepository.class);
        servicios = mock(ServicioRepository.class);
        usuarios = mock(UsuarioRepository.class);
        clientes = mock(ClienteService.class);

        Clock reloj = Clock.fixed(
                Instant.parse("2026-11-16T15:00:00Z"),
                ZoneId.of("America/Bogota")
        );

        servicioCitas = new CitaService(
                citas,
                salones,
                servicios,
                usuarios,
                clientes,
                new ReglasAgenda(),
                new CitaMapper(),
                reloj
        );

        salon = new Salon();
        salon.setId(1L);
        salon.setConfiguracion(
                ConfiguracionSalon.porDefecto()
        );

        corte = new Servicio();
        corte.setId(10L);
        corte.setNombre("Corte clásico");
        corte.setDuracionMinutos(45);
        corte.setPrecio(40000);
        corte.setActivo(true);

        estilista = new Usuario();
        estilista.setId(20L);
        estilista.setNombre("Sara Mejía");
        estilista.setRol(Rol.ESTILISTA);
        estilista.setActivo(true);
        estilista.setSalon(salon);
        estilista.setServicios(Set.of(corte));
    }

    private void prepararReservaValida() {

        when(salones.findByIdParaAgendar(1L))
                .thenReturn(Optional.of(salon));

        when(servicios.findByIdAndSalonId(10L, 1L))
                .thenReturn(Optional.of(corte));

        when(usuarios.findById(20L))
                .thenReturn(Optional.of(estilista));

        Cliente cliente = new Cliente(
                "Carlos Pérez",
                "3001234567",
                salon
        );

        when(clientes.buscarOCrear(
                "Carlos Pérez",
                "3001234567",
                salon
        )).thenReturn(cliente);

        when(citas.saveAndFlush(any(Cita.class)))
                .thenAnswer(invocacion -> {
                    Cita cita = invocacion.getArgument(0);
                    cita.setId(99L);
                    return cita;
                });
    }

    private CitaRequest crearSolicitud(
            LocalDateTime inicio
    ) {
        return new CitaRequest(
                "Carlos Pérez",
                "3001234567",
                10L,
                20L,
                inicio,
                null
        );
    }

    // PRUEBA 1
    // Una cita válida debe quedar CONFIRMADA
    // y conservar el precio del servicio.

    @Test
    void citaValidaQuedaConfirmadaConPrecioDelServicio() {

        prepararReservaValida();

        CitaRequest solicitud = crearSolicitud(
                LocalDateTime.of(2026, 11, 17, 10, 0)
        );

        CitaResponse respuesta =
                servicioCitas.crear(1L, solicitud);

        assertEquals(
                EstadoCita.CONFIRMADA,
                respuesta.estado()
        );

        assertEquals(40000, respuesta.precio());

        assertEquals(
                OrigenCita.SALON,
                respuesta.origen()
        );

        assertEquals(
                LocalDateTime.of(2026, 11, 17, 10, 45),
                respuesta.fin()
        );

        verify(citas, times(1))
                .saveAndFlush(any(Cita.class));
    }

    // PRUEBA 2
    // Una cita en el pasado no debe guardarse.

    @Test
    void citaEnElPasadoNoDebeGuardarse() {

        prepararReservaValida();

        CitaRequest solicitud = crearSolicitud(
                LocalDateTime.of(2026, 11, 16, 9, 0)
        );

        assertThrows(
                ReglaNegocioException.class,
                () -> servicioCitas.crear(1L, solicitud)
        );

        verify(citas, never())
                .saveAndFlush(any(Cita.class));

        verifyNoInteractions(clientes);
    }

    // PRUEBA 3
    // Una cita que se cruza con otra
    // reserva no debe guardarse.

    @Test
    void citaConCruceNoDebeGuardarse() {

        prepararReservaValida();

        when(citas
                .existsBySalonIdAndEstilistaIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
                        eq(1L),
                        eq(20L),
                        anyCollection(),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                ))
                .thenReturn(true);

        CitaRequest solicitud = crearSolicitud(
                LocalDateTime.of(2026, 11, 17, 10, 0)
        );

        assertThrows(
                ReglaNegocioException.class,
                () -> servicioCitas.crear(1L, solicitud)
        );

        verify(citas, never())
                .saveAndFlush(any(Cita.class));

        verifyNoInteractions(clientes);
    }

    // PRUEBA 4
    // Un estilista no puede atender
    // servicios que no tiene asignados.

    @Test
    void estilistaQueNoOfreceServicioNoDebeGuardarCita() {

        prepararReservaValida();

        Servicio barba = new Servicio();
        barba.setId(11L);
        barba.setNombre("Arreglo de barba");

        estilista.setServicios(Set.of(barba));

        CitaRequest solicitud = crearSolicitud(
                LocalDateTime.of(2026, 11, 17, 10, 0)
        );

        assertThrows(
                ReglaNegocioException.class,
                () -> servicioCitas.crear(1L, solicitud)
        );

        verify(citas, never())
                .saveAndFlush(any(Cita.class));

        verifyNoInteractions(clientes);
    }

    // PRUEBA 5
    // Un estilista no puede modificar
    // una cita que pertenece a otro estilista.

    @Test
    void estilistaNoPuedeModificarCitaAjena() {

        Usuario otroEstilista = new Usuario();
        otroEstilista.setId(21L);
        otroEstilista.setRol(Rol.ESTILISTA);

        Cita cita = new Cita();
        cita.setId(99L);
        cita.setEstilista(otroEstilista);
        cita.setEstado(EstadoCita.CONFIRMADA);

        when(citas.findByIdAndSalonId(99L, 1L))
                .thenReturn(Optional.of(cita));

        CambiarEstadoCitaRequest solicitud =
                new CambiarEstadoCitaRequest(
                        EstadoCita.COMPLETADA
                );

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> servicioCitas.actualizarEstado(
                        1L,
                        estilista.getId(),
                        Rol.ESTILISTA,
                        99L,
                        solicitud
                )
        );

        verify(citas, never())
                .saveAndFlush(any(Cita.class));
    }

    // PRUEBA 6
    // Un estilista no puede cancelar
    // sus propias citas.

    @Test
    void estilistaNoPuedeCancelarSuPropiaCita() {

        Cita cita = new Cita();
        cita.setId(99L);
        cita.setEstilista(estilista);
        cita.setEstado(EstadoCita.CONFIRMADA);

        when(citas.findByIdAndSalonId(99L, 1L))
                .thenReturn(Optional.of(cita));

        CambiarEstadoCitaRequest solicitud =
                new CambiarEstadoCitaRequest(
                        EstadoCita.CANCELADA
                );

        assertThrows(
                AccionNoPermitidaException.class,
                () -> servicioCitas.actualizarEstado(
                        1L,
                        estilista.getId(),
                        Rol.ESTILISTA,
                        99L,
                        solicitud
                )
        );

        assertEquals(
                EstadoCita.CONFIRMADA,
                cita.getEstado()
        );

        verify(citas, never())
                .saveAndFlush(any(Cita.class));
    }


    @Test
    void transicionNoPermitidaNoDebeGuardarCita() {

        Cita cita = new Cita();
        cita.setId(99L);
        cita.setEstilista(estilista);
        cita.setEstado(EstadoCita.COMPLETADA);

        when(citas.findByIdAndSalonId(99L, 1L))
                    .thenReturn(Optional.of(cita));

        CambiarEstadoCitaRequest solicitud =
                    new CambiarEstadoCitaRequest(
                        EstadoCita.CONFIRMADA
                    );

        assertThrows(
            ReglaNegocioException.class,
            () -> servicioCitas.actualizarEstado(
                    1L,
                    estilista.getId(),
                    Rol.DUENO,
                    99L,
                    solicitud
                )
        );

        assertEquals(
                EstadoCita.COMPLETADA,
                cita.getEstado()
        );

        verify(citas, never())
                .saveAndFlush(any(Cita.class));
    }
}
