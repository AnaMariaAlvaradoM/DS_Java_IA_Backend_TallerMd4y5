package com.devsenior.sillon.cita;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.usuario.Usuario;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DatosRecordatorioTest {
    private final CitaRepository repositorio = mock(CitaRepository.class);
    private final CitaService servicio = new CitaService(repositorio, null, null, null, null, null,
            Clock.fixed(Instant.parse("2026-10-19T12:00:00Z"), ZoneId.of("America/Bogota")));
    private Cita cita;

    @BeforeEach void preparar() {
        Salon salon = new Salon(); salon.setNombre("La Navaja"); salon.setDireccion("Calle 10 # 43-20"); salon.setCiudad("Medellín");
        Servicio corte = new Servicio(); corte.setNombre("Corte clásico"); corte.setDuracionMinutos(45); corte.setPrecio(50000);
        Usuario sara = new Usuario(); sara.setNombre("Sara Mejía");
        Cliente cliente = new Cliente(); cliente.setNombre("Andrés"); cliente.setTelefono("+573102223344");
        cita = new Cita(); cita.setId(5L); cita.setSalon(salon); cita.setServicio(corte); cita.setEstilista(sara); cita.setCliente(cliente);
        cita.setInicio(LocalDateTime.of(2026, 10, 20, 10, 30)); cita.setFin(cita.getInicio().plusMinutes(45));
        cita.setPrecio(35000); cita.setEstado(EstadoCita.CONFIRMADA);
        when(repositorio.findOneByIdAndSalonId(5L, 1L)).thenReturn(Optional.of(cita));
    }

    @ParameterizedTest @EnumSource(value = EstadoCita.class, names = {"PENDIENTE", "CONFIRMADA"})
    void datosSonDeLaCitaYSeFormateanEnColombia(EstadoCita estado) {
        cita.setEstado(estado);
        DatosRecordatorio datos = servicio.datosRecordatorio(1L, 5L);
        assertEquals("La Navaja", datos.salonNombre()); assertEquals("Andrés", datos.clienteNombre());
        assertEquals("+573102223344", datos.telefonoCliente());
        assertEquals("""
                Servicio: Corte clásico (45 minutos)
                Estilista: Sara Mejía
                Fecha: martes 20 de octubre
                Hora: 10:30 a. m.
                Valor: $35.000
                Dirección: Calle 10 # 43-20, Medellín""", datos.datos());
        assertFalse(datos.datos().contains("\u00a0")); assertFalse(datos.datos().contains("\u202f"));
        verify(repositorio, never()).save(any());
    }

    @ParameterizedTest @EnumSource(value = EstadoCita.class, names = {"COMPLETADA", "CANCELADA", "NO_ASISTIO"})
    void citaFinalEs409(EstadoCita estado) {
        cita.setEstado(estado);
        assertThrows(ReglaNegocioException.class, () -> servicio.datosRecordatorio(1L, 5L));
    }

    @Test void citaAjenaONoExistenteEs404() {
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.datosRecordatorio(2L, 5L));
        verify(repositorio).findOneByIdAndSalonId(5L, 2L);
    }
}
