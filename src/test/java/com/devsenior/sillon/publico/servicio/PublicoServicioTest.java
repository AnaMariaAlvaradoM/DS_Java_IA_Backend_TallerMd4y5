
package com.devsenior.sillon.publico.servicio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devsenior.sillon.cita.dto.RespuestaCita;
import com.devsenior.sillon.cita.dto.SolicitudCrearCita;
import com.devsenior.sillon.cita.modelo.Cita;
import com.devsenior.sillon.cita.modelo.EstadoCita;
import com.devsenior.sillon.cita.repositorio.CitaRepositorio;
import com.devsenior.sillon.cita.servicio.CitaServicio;
import com.devsenior.sillon.cita.servicio.ReglasAgenda;
import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.publico.dto.RespuestaCatalogoPublico;
import com.devsenior.sillon.publico.dto.RespuestaDisponibilidad;
import com.devsenior.sillon.publico.mapper.PublicoMapper;
import com.devsenior.sillon.salon.modelo.ConfiguracionSalon;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;
import com.devsenior.sillon.servicio.modelo.Servicio;
import com.devsenior.sillon.servicio.repositorio.ServicioRepositorio;
import com.devsenior.sillon.usuario.modelo.Rol;
import com.devsenior.sillon.usuario.modelo.Usuario;
import com.devsenior.sillon.usuario.repositorio.UsuarioRepositorio;

class PublicoServicioTest {

    private SalonRepositorio salonRepositorio;
    private ServicioRepositorio servicioRepositorio;
    private UsuarioRepositorio usuarioRepositorio;
    private CitaRepositorio citaRepositorio;
    private CitaServicio citaServicio;
    private PublicoServicio publicoServicio;

    private Salon salon;
    private Servicio corte;
    private Usuario mateo;

    private static final String SLUG = "barberia-pruebas";
    private static final LocalDate FECHA =
            LocalDate.of(2026, 11, 26);

    @BeforeEach
    void preparar() {
        salonRepositorio = mock(SalonRepositorio.class);
        servicioRepositorio = mock(ServicioRepositorio.class);
        usuarioRepositorio = mock(UsuarioRepositorio.class);
        citaRepositorio = mock(CitaRepositorio.class);
        citaServicio = mock(CitaServicio.class);

        Clock reloj = Clock.fixed(
                Instant.parse("2026-11-24T15:00:00Z"),
                ZoneId.of("America/Bogota"));

        publicoServicio = new PublicoServicio(
                salonRepositorio,
                servicioRepositorio,
                usuarioRepositorio,
                citaRepositorio,
                new ReglasAgenda(reloj),
                citaServicio,
                new PublicoMapper());

        salon = new Salon();
        salon.setId(2L);
        salon.setNombre("Barberia Pruebas");
        salon.setSlug(SLUG);
        salon.setCiudad("Bogota");
        salon.setDireccion("Calle 100");
        salon.setTelefono("3001234567");
        salon.setConfiguracion(
                ConfiguracionSalon.predeterminada());

        corte = crearServicio(
                1L, "Corte clasico", 45, 40000, true);

        mateo = crearEstilista(
                3L, "Mateo Gil", List.of(corte));

        when(salonRepositorio.findBySlug(SLUG))
                .thenReturn(Optional.of(salon));
    }

    private Servicio crearServicio(
            Long id, String nombre, int duracion,
            int precio, boolean activo) {

        Servicio servicio = new Servicio();
        servicio.setId(id);
        servicio.setNombre(nombre);
        servicio.setDescripcion("Servicio de prueba");
        servicio.setDuracionMinutos(duracion);
        servicio.setPrecio(precio);
        servicio.setActivo(activo);
        servicio.setSalon(salon);

        return servicio;
    }

    private Usuario crearEstilista(
            Long id, String nombre, List<Servicio> servicios) {

        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre(nombre);
        usuario.setRol(Rol.ESTILISTA);
        usuario.setActivo(true);
        usuario.setSalon(salon);
        usuario.setServicios(servicios);

        return usuario;
    }

    private void prepararDisponibilidad() {
        when(servicioRepositorio.findByIdAndSalonId(1L, 2L))
                .thenReturn(Optional.of(corte));

        when(usuarioRepositorio.buscarEstilistaConServiciosPorId(
                3L, 2L, Rol.ESTILISTA))
                .thenReturn(Optional.of(mateo));
    }

    @Test
    void catalogoIncluyeDosServiciosYDosEstilistas()
            throws Exception {

        Servicio barba = crearServicio(
                2L, "Barba", 30, 20000, true);

        Usuario sara = crearEstilista(
                4L, "Sara", List.of(corte));

        when(servicioRepositorio
                .findAllBySalonIdAndActivoTrueOrderByIdAsc(2L))
                .thenReturn(List.of(corte, barba));

        when(usuarioRepositorio
                .buscarEstilistasActivosConServicios(
                        2L, Rol.ESTILISTA))
                .thenReturn(List.of(mateo, sara));

        RespuestaCatalogoPublico respuesta =
                publicoServicio.consultarCatalogo(SLUG);

        assertEquals(2, respuesta.servicios().size());
        assertEquals(2, respuesta.estilistas().size());
        assertEquals("Barberia Pruebas", respuesta.nombre());
        assertEquals(
                "08:00 - 19:00 (lunes a sábado)",
                respuesta.horario());
        assertEquals(List.of(1L),
                respuesta.estilistas().get(0).servicioIds());
    }

    @Test
    void catalogoFiltraServiciosInactivos()
            throws Exception {

        Servicio inactivo = crearServicio(
                2L, "Servicio inactivo", 30, 20000, false);

        mateo.setServicios(List.of(corte, inactivo));

        when(servicioRepositorio
                .findAllBySalonIdAndActivoTrueOrderByIdAsc(2L))
                .thenReturn(List.of(corte));

        when(usuarioRepositorio
                .buscarEstilistasActivosConServicios(
                        2L, Rol.ESTILISTA))
                .thenReturn(List.of(mateo));

        RespuestaCatalogoPublico respuesta =
                publicoServicio.consultarCatalogo(SLUG);

        assertEquals(1, respuesta.servicios().size());
        assertEquals(List.of(1L),
                respuesta.estilistas().get(0).servicioIds());
    }

    @Test
    void salonInexistenteGeneraError404()
            throws Exception {

        when(salonRepositorio.findBySlug("inexistente"))
                .thenReturn(Optional.empty());

        assertThrows(
                ExcepcionRecursoNoEncontrado.class,
                () -> publicoServicio.consultarCatalogo(
                        "inexistente"));

        verifyNoInteractions(servicioRepositorio,
                usuarioRepositorio);
    }

    @Test
    void disponibilidadInicialIncluyeLasDiez()
            throws Exception {

        prepararDisponibilidad();

        when(citaRepositorio.buscarCitasActivasEstilistaEnRango(
                eq(2L), eq(3L), any(),
                any(), any()))
                .thenReturn(List.of());

        RespuestaDisponibilidad respuesta =
                publicoServicio.consultarDisponibilidad(
                        SLUG, 1L, 3L, FECHA);

        assertEquals(45, respuesta.duracionMinutos());
        assertTrue(respuesta.horariosLibres()
                .contains("10:00:00"));
        assertTrue(respuesta.horariosLibres()
                .contains("09:30:00"));
    }

    @Test
    void reservaExistenteBloqueaHorariosSuperpuestos()
            throws Exception {

        prepararDisponibilidad();

        Cita cita = new Cita();
        cita.setInicio(FECHA.atTime(10, 0));
        cita.setFin(FECHA.atTime(10, 45));
        cita.setEstado(EstadoCita.PENDIENTE);

        when(citaRepositorio.buscarCitasActivasEstilistaEnRango(
                eq(2L), eq(3L), any(),
                any(), any()))
                .thenReturn(List.of(cita));

        RespuestaDisponibilidad respuesta =
                publicoServicio.consultarDisponibilidad(
                        SLUG, 1L, 3L, FECHA);

        List<String> horarios = respuesta.horariosLibres();

        assertFalse(horarios.contains("09:30:00"));
        assertFalse(horarios.contains("10:00:00"));
        assertFalse(horarios.contains("10:30:00"));
        assertTrue(horarios.contains("11:00:00"));
    }

    @Test
    void servicioDeOtroSalonGenera404()
            throws Exception {

        when(servicioRepositorio.findByIdAndSalonId(1L, 2L))
                .thenReturn(Optional.empty());

        assertThrows(
                ExcepcionRecursoNoEncontrado.class,
                () -> publicoServicio.consultarDisponibilidad(
                        SLUG, 1L, 3L, FECHA));
    }

    @Test
    void servicioInactivoGenera409()
            throws Exception {

        corte.setActivo(false);

        when(servicioRepositorio.findByIdAndSalonId(1L, 2L))
                .thenReturn(Optional.of(corte));

        assertThrows(
                ExcepcionConflicto.class,
                () -> publicoServicio.consultarDisponibilidad(
                        SLUG, 1L, 3L, FECHA));
    }

    @Test
    void estilistaQueNoOfreceServicioGenera409()
            throws Exception {

        prepararDisponibilidad();
        mateo.setServicios(List.of());

        assertThrows(
                ExcepcionConflicto.class,
                () -> publicoServicio.consultarDisponibilidad(
                        SLUG, 1L, 3L, FECHA));
    }

    @Test
    void domingoCerradoNoTieneHorariosDisponibles()
            throws Exception {

        prepararDisponibilidad();

        when(citaRepositorio.buscarCitasActivasEstilistaEnRango(
                eq(2L), eq(3L), any(),
                any(), any()))
                .thenReturn(List.of());

        LocalDate domingo = LocalDate.of(2026, 11, 29);

        RespuestaDisponibilidad respuesta =
                publicoServicio.consultarDisponibilidad(
                        SLUG, 1L, 3L, domingo);

        assertTrue(respuesta.horariosLibres().isEmpty());
    }

    @Test
    void reservarDelegaEnCitaServicio()
            throws Exception {

        SolicitudCrearCita solicitud =
                mock(SolicitudCrearCita.class);
        RespuestaCita respuesta =
                mock(RespuestaCita.class);

        when(citaServicio.crearReservaPublica(
                SLUG, solicitud))
                .thenReturn(respuesta);

        RespuestaCita resultado =
                publicoServicio.reservar(SLUG, solicitud);

        assertSame(respuesta, resultado);
        verify(citaServicio).crearReservaPublica(
                SLUG, solicitud);
    }
}
