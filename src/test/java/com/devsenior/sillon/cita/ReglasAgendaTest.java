package com.devsenior.sillon.cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

class ReglasAgendaTest {
    private final ReglasAgenda reglas = new ReglasAgenda(Clock.fixed(
            Instant.parse("2026-10-19T12:00:00Z"), ZoneId.of("America/Bogota")));
    private final ConfiguracionSalon horario = ConfiguracionSalon.porDefecto();

    @Test
    void permiteCitaQueTerminaEnElCierre() {
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 20, 18, 15);
        reglas.validarHorario(horario, inicio, inicio.plusMinutes(45));
    }

    @Test
    void rechazaCitaQueTerminaDespuesDelCierre() {
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 20, 18, 45);
        assertThrows(ReglaNegocioException.class,
                () -> reglas.validarHorario(horario, inicio, inicio.plusMinutes(45)));
    }

    @Test
    void rechazaDomingoCerrado() {
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 25, 10, 0);
        assertThrows(ReglaNegocioException.class,
                () -> reglas.validarHorario(horario, inicio, inicio.plusMinutes(45)));
    }

    @Test
    void citaContiguaNoSeCruza() {
        LocalDateTime primera = LocalDateTime.of(2026, 10, 20, 10, 0);
        assertFalse(reglas.cruzan(primera, primera.plusMinutes(45),
                primera.plusMinutes(45), primera.plusMinutes(75)));
    }

    @Test
    void disponibilidadExcluyeTodosLosIniciosQueCruzan() {
        Cita ocupada = new Cita();
        ocupada.setInicio(LocalDateTime.of(2026, 10, 20, 10, 0));
        ocupada.setFin(LocalDateTime.of(2026, 10, 20, 10, 45));
        ocupada.setEstado(EstadoCita.PENDIENTE);

        List<LocalTime> libres = reglas.horariosLibres(horario, LocalDate.of(2026, 10, 20),
                45, List.of(ocupada));

        assertFalse(libres.contains(LocalTime.of(9, 30)));
        assertFalse(libres.contains(LocalTime.of(10, 0)));
        assertFalse(libres.contains(LocalTime.of(10, 30)));
        assertTrue(libres.contains(LocalTime.of(11, 0)));
    }

    @Test
    void domingoCerradoNoOfreceTurnos() {
        assertEquals(List.of(), reglas.horariosLibres(horario,
                LocalDate.of(2026, 10, 25), 45, List.of()));
    }
}
