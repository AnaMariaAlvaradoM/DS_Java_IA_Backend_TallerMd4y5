package com.devsenior.sillon.cita;

import com.devsenior.sillon.compartido.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReglasAgendaTest {
    private static final LocalDate FECHA_FUTURA = LocalDate.of(2026, 11, 16);
    private static final LocalTime APERTURA = LocalTime.of(8, 0);
    private static final LocalTime CIERRE = LocalTime.of(19, 0);
    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    private ReglasAgenda reglasAgenda;

    @BeforeEach
    void configurarRelojFijo() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-13T15:15:00Z"), ZONA_BOGOTA);
        reglasAgenda = new ReglasAgenda(reloj);
    }

    @Test
    void aceptaCitaDentroDelHorarioLaboral() {
        assertDoesNotThrow(() -> reglasAgenda.validarHorario(
                FECHA_FUTURA.atTime(10, 0), 60, APERTURA, CIERRE, false));
    }

    @Test
    void rechazaCitaQueTerminaDespuesDelCierre() {
        assertThrows(ReglaNegocioException.class, () -> reglasAgenda.validarHorario(
                FECHA_FUTURA.atTime(18, 30), 60, APERTURA, CIERRE, false));
    }

    @Test
    void rechazaCitaQueEmpiezaAntesDeLaApertura() {
        assertThrows(ReglaNegocioException.class, () -> reglasAgenda.validarHorario(
                FECHA_FUTURA.atTime(7, 30), 30, APERTURA, CIERRE, false));
    }

    @Test
    void rechazaDomingoCuandoElSalonEstaCerrado() {
        LocalDate domingo = LocalDate.of(2026, 11, 15);
        assertThrows(ReglaNegocioException.class, () -> reglasAgenda.validarHorario(
                domingo.atTime(10, 0), 30, APERTURA, CIERRE, false));
    }

    @Test
    void horariosLibresExcluyenLosCrucesConCitasExistentes() {
        LocalDateTime ocupadaInicio = FECHA_FUTURA.atTime(9, 0);
        List<ReglasAgenda.IntervaloOcupado> ocupadas = List.of(
                new ReglasAgenda.IntervaloOcupado(ocupadaInicio, ocupadaInicio.plusMinutes(60)));

        List<LocalTime> horarios = reglasAgenda.calcularHorariosLibres(
                FECHA_FUTURA, 30, APERTURA, CIERRE, false, ocupadas,
                FECHA_FUTURA.atStartOfDay());

        assertFalse(horarios.contains(LocalTime.of(9, 0)));
        assertFalse(horarios.contains(LocalTime.of(9, 30)));
        assertTrue(horarios.contains(LocalTime.of(10, 0)));
    }

    @Test
    void domingoCerradoNoTieneHorariosDisponibles() {
        LocalDate domingo = LocalDate.of(2026, 11, 15);
        assertEquals(List.of(), reglasAgenda.calcularHorariosLibres(
                domingo, 30, APERTURA, CIERRE, false, List.of(), domingo.atStartOfDay()));
    }

    @Test
    void hoyNoOfreceHorasQueYaPasaronSegunElRelojFijo() {
        LocalDate hoy = LocalDate.now(Clock.fixed(
                Instant.parse("2026-11-13T15:15:00Z"), ZONA_BOGOTA));
        List<LocalTime> horarios = reglasAgenda.calcularHorariosLibres(
                hoy, 30, APERTURA, CIERRE, false, List.of(), null);

        assertFalse(horarios.contains(LocalTime.of(10, 0)));
        assertTrue(horarios.contains(LocalTime.of(10, 30)));
    }
}
