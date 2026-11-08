package com.devsenior.sillon.cita;

import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ReglasAgendaTests {
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T14:00:00Z"), ZoneId.of("America/Bogota"));
    private final ReglasAgenda reglas = new ReglasAgenda(clock);
    private final ConfiguracionSalon configuracion = ConfiguracionSalon.porDefecto();

    @ParameterizedTest
    @CsvSource({"PENDIENTE,CONFIRMADA", "PENDIENTE,CANCELADA", "CONFIRMADA,COMPLETADA",
            "CONFIRMADA,CANCELADA", "CONFIRMADA,NO_ASISTIO"})
    void transicionesValidas(EstadoCita actual, EstadoCita destino) {
        assertTrue(actual.puedeCambiarA(destino));
    }

    @ParameterizedTest
    @CsvSource({"PENDIENTE,COMPLETADA", "PENDIENTE,NO_ASISTIO", "CONFIRMADA,PENDIENTE",
            "COMPLETADA,CONFIRMADA", "CANCELADA,PENDIENTE", "NO_ASISTIO,COMPLETADA"})
    void transicionesInvalidas(EstadoCita actual, EstadoCita destino) {
        assertFalse(actual.puedeCambiarA(destino));
    }

    @Test
    void soloPendienteYConfirmadaOcupanAgenda() {
        assertTrue(EstadoCita.PENDIENTE.ocupaAgenda());
        assertTrue(EstadoCita.CONFIRMADA.ocupaAgenda());
        assertFalse(EstadoCita.COMPLETADA.ocupaAgenda());
        assertFalse(EstadoCita.CANCELADA.ocupaAgenda());
        assertFalse(EstadoCita.NO_ASISTIO.ocupaAgenda());
    }

    @Test
    void horarioPasadoDomingoYCruces() {
        LocalDate martes = LocalDate.of(2026, 10, 20);
        reglas.validarHorario(configuracion, martes.atTime(10, 0), martes.atTime(10, 45));
        assertThrows(ReglaNegocioException.class,
                () -> reglas.validarNoPasado(martes.atTime(8, 59)));
        assertThrows(ReglaNegocioException.class,
                () -> reglas.validarHorario(configuracion, martes.atTime(7, 45), martes.atTime(8, 30)));
        assertThrows(ReglaNegocioException.class,
                () -> reglas.validarHorario(configuracion, martes.atTime(18, 45), martes.atTime(19, 30)));
        assertThrows(ReglaNegocioException.class,
                () -> reglas.validarHorario(configuracion, LocalDate.of(2026, 10, 25).atTime(10, 0),
                        LocalDate.of(2026, 10, 25).atTime(10, 45)));
        assertTrue(reglas.seCruzan(martes.atTime(9, 30), martes.atTime(10, 15),
                martes.atTime(10, 0), martes.atTime(10, 45)));
        assertFalse(reglas.seCruzan(martes.atTime(10, 0), martes.atTime(10, 45),
                martes.atTime(10, 45), martes.atTime(11, 15)));
    }

    @Test
    void disponibilidadExcluyeCrucesFinalesHorasPasadasYDomingo() {
        LocalDate martes = LocalDate.of(2026, 10, 20);
        Cita ocupada = cita(martes.atTime(10, 0), martes.atTime(10, 45), EstadoCita.PENDIENTE);
        Cita completada = cita(martes.atTime(11, 0), martes.atTime(11, 45), EstadoCita.COMPLETADA);
        List<LocalTime> libres = reglas.horariosLibres(configuracion, martes, 45, List.of(ocupada, completada));
        assertFalse(libres.contains(LocalTime.of(8, 30)));
        assertFalse(libres.contains(LocalTime.of(9, 30)));
        assertFalse(libres.contains(LocalTime.of(10, 0)));
        assertFalse(libres.contains(LocalTime.of(10, 30)));
        assertTrue(libres.contains(LocalTime.of(11, 0)));
        assertTrue(reglas.horariosLibres(configuracion, LocalDate.of(2026, 10, 25), 45, List.of()).isEmpty());
    }

    private Cita cita(LocalDateTime inicio, LocalDateTime fin, EstadoCita estado) {
        Cita cita = new Cita();
        cita.setInicio(inicio);
        cita.setFin(fin);
        cita.setEstado(estado);
        return cita;
    }
}
