package com.devsenior.sillon.salon.servicio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.salon.dto.ConfiguracionSalonDto;
import com.devsenior.sillon.salon.dto.RespuestaSalon;
import com.devsenior.sillon.salon.mapper.SalonMapper;
import com.devsenior.sillon.salon.modelo.ConfiguracionSalon;
import com.devsenior.sillon.salon.modelo.Plan;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;

class SalonServicioTest {

    private SalonRepositorio salonRepositorio;
    private SalonServicio salonServicio;
    private Salon salon;

    @BeforeEach
    void preparar() {

        salonRepositorio = mock(SalonRepositorio.class);

        salonServicio = new SalonServicio(
                salonRepositorio,
                new SalonMapper());

        salon = new Salon();

        salon.setNombre("Barbería La Navaja");
        salon.setSlug("barberia-la-navaja");
        salon.setCiudad("Manizales");
        salon.setDireccion("Calle 23");
        salon.setTelefono("3001234567");
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(10);
        salon.setConfiguracion(
                ConfiguracionSalon.predeterminada());
        salon.setFechaCreacion(
                LocalDateTime.of(2026, 11, 19, 10, 0));
    }

    @Test
    void consultarDevuelveInformacionDelSalon() {

        when(salonRepositorio.findById(1L))
                .thenReturn(Optional.of(salon));

        RespuestaSalon respuesta = salonServicio.consultar(1L);

        assertEquals("Barbería La Navaja", respuesta.nombre());
        assertEquals("barberia-la-navaja", respuesta.slug());
        assertEquals(Plan.GRATIS, respuesta.plan());
        assertEquals(10, respuesta.creditosIa());

        assertEquals(
                LocalTime.of(8, 0),
                respuesta.configuracion().horaApertura());

        assertEquals(
                LocalTime.of(19, 0),
                respuesta.configuracion().horaCierre());

        verify(salonRepositorio).findById(1L);
    }

    @Test
    void actualizarConfiguracionGuardaNuevosValores() {

        ConfiguracionSalonDto solicitud =
                new ConfiguracionSalonDto(
                        LocalTime.of(9, 0),
                        LocalTime.of(20, 0),
                        true,
                        45);

        when(salonRepositorio.findById(1L))
                .thenReturn(Optional.of(salon));

        when(salonRepositorio.save(salon))
                .thenReturn(salon);

        RespuestaSalon respuesta =
                salonServicio.actualizarConfiguracion(
                        1L, solicitud);

        assertEquals(
                LocalTime.of(9, 0),
                respuesta.configuracion().horaApertura());

        assertEquals(
                LocalTime.of(20, 0),
                respuesta.configuracion().horaCierre());

        assertTrue(respuesta.configuracion().abreDomingos());

        assertEquals(
                45,
                respuesta.configuracion().intervaloMinutos());

        assertEquals(Plan.GRATIS, respuesta.plan());
        assertEquals(10, respuesta.creditosIa());

        verify(salonRepositorio).save(salon);
    }

    @Test
    void actualizarConfiguracionRechazaHorarioInvalido() {

        ConfiguracionSalonDto solicitud =
                new ConfiguracionSalonDto(
                        LocalTime.of(20, 0),
                        LocalTime.of(9, 0),
                        true,
                        45);

        assertThrows(
                ExcepcionConflicto.class,
                () -> salonServicio.actualizarConfiguracion(
                        1L, solicitud));

        verifyNoInteractions(salonRepositorio);
    }

    @Test
    void consultarRechazaSalonInexistente() {

        when(salonRepositorio.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ExcepcionRecursoNoEncontrado.class,
                () -> salonServicio.consultar(999L));

        verify(salonRepositorio).findById(999L);
    }
}