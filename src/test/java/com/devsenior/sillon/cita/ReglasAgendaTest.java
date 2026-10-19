package com.devsenior.sillon.cita;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReglasAgendaTest {
    private final ReglasAgenda reglas = new ReglasAgenda(Clock.fixed(
            Instant.parse("2026-10-19T15:15:00Z"), ZoneId.of("America/Bogota")));
    private final ConfiguracionSalon config = ConfiguracionSalon.porDefecto();
    private final LocalDate martes = LocalDate.of(2026, 10, 20);

    @Test void horarioValidoIncluyeLosLimites() {
        assertDoesNotThrow(() -> reglas.validarHorario(config, martes.atTime(8, 0), martes.atTime(19, 0)));
    }
    @Test void terminaDespuesDelCierreEs409ConHorario() {
        var error = assertThrows(ReglaNegocioException.class,
                () -> reglas.validarHorario(config, martes.atTime(18, 45), martes.atTime(19, 30)));
        assertTrue(error.getMessage().contains("08:00"));
        assertTrue(error.getMessage().contains("19:00"));
    }
    @Test void empiezaAntesDeAbrirEs409() {
        assertThrows(ReglaNegocioException.class,
                () -> reglas.validarHorario(config, martes.atTime(7, 45), martes.atTime(8, 30)));
    }
    @Test void domingoCerradoEs409YNoTieneHorarios() {
        LocalDate domingo = LocalDate.of(2026, 10, 25);
        assertThrows(ReglaNegocioException.class,
                () -> reglas.validarHorario(config, domingo.atTime(10, 0), domingo.atTime(10, 45)));
        assertTrue(reglas.horariosLibres(config, domingo, 45, List.of()).isEmpty());
    }
    @Test void domingoAbiertoPermiteAgendar() {
        config.setAbreDomingos(true);
        LocalDate domingo = LocalDate.of(2026, 10, 25);
        assertDoesNotThrow(() -> reglas.validarHorario(config, domingo.atTime(10, 0), domingo.atTime(10, 45)));
        assertFalse(reglas.horariosLibres(config, domingo, 45, List.of()).isEmpty());
    }
    @Test void citaNoPuedePasarAlDiaSiguiente() {
        assertThrows(ReglaNegocioException.class,
                () -> reglas.validarHorario(config, martes.atTime(18, 0), martes.plusDays(1).atTime(9, 0)));
    }
    @Test void disponibilidadExcluyeCrucesYRespetaCuadricula() {
        Cita cita = ocupada(martes.atTime(10, 0), EstadoCita.PENDIENTE);
        List<LocalTime> libres = reglas.horariosLibres(config, martes, 45, List.of(cita));
        assertFalse(libres.contains(LocalTime.of(9, 30)));
        assertFalse(libres.contains(LocalTime.of(10, 0)));
        assertFalse(libres.contains(LocalTime.of(10, 30)));
        assertTrue(libres.contains(LocalTime.of(11, 0)));
        assertEquals(LocalTime.of(18, 0), libres.get(libres.size() - 1));
    }
    @Test void hoyUsaLaHoraDeColombiaYExcluyePasado() {
        var libres = reglas.horariosLibres(config, LocalDate.of(2026, 10, 19), 45, List.of());
        assertEquals(LocalTime.of(10, 30), libres.get(0));
    }
    @Test void citaFinalNoBloqueaAgenda() {
        Cita cita = ocupada(martes.atTime(10, 0), EstadoCita.CANCELADA);
        assertTrue(reglas.horariosLibres(config, martes, 45, List.of(cita)).contains(LocalTime.of(10, 0)));
    }
    @Test void intervalosContiguosNoSeCruzan() {
        assertFalse(reglas.seCruzan(martes.atTime(10, 0), martes.atTime(10, 45),
                martes.atTime(10, 45), martes.atTime(11, 30)));
    }
    private Cita ocupada(LocalDateTime inicio, EstadoCita estado) {
        Cita cita = new Cita();
        cita.setInicio(inicio); cita.setFin(inicio.plusMinutes(45)); cita.setEstado(estado);
        return cita;
    }
}
