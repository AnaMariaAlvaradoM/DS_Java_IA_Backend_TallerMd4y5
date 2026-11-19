package com.devsenior.sillon.cita.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.model.ConfiguracionSalon;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Reloj fijo: lunes 19 de octubre de 2026, 9:10 a. m. en Bogotá. Mañana es martes 20; el domingo es el 25. */
class ReglasAgendaTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final LocalDate MARTES = LocalDate.of(2026, 10, 20);
    private static final LocalDate DOMINGO = LocalDate.of(2026, 10, 25);

    private final Clock reloj = Clock.fixed(HOY.atTime(9, 10).atZone(BOGOTA).toInstant(), BOGOTA);
    private final ReglasAgenda reglas = new ReglasAgenda(reloj);
    private final ConfiguracionSalon horario = ConfiguracionSalon.porDefecto();

    @Test
    void validarHorario_dentroDelHorario_noLanza() {
        LocalDateTime inicio = MARTES.atTime(10, 30);
        assertDoesNotThrow(() -> reglas.validarHorario(horario, inicio, inicio.plusMinutes(45)));
    }

    @Test
    void validarHorario_terminaDespuesDelCierre_lanza409() {
        LocalDateTime inicio = MARTES.atTime(18, 45);
        assertThrows(ReglaNegocioException.class, () -> reglas.validarHorario(horario, inicio, inicio.plusMinutes(45)));
    }

    @Test
    void validarHorario_terminaJustoAlCierre_noLanza() {
        LocalDateTime inicio = MARTES.atTime(18, 15);
        assertDoesNotThrow(() -> reglas.validarHorario(horario, inicio, inicio.plusMinutes(45)));
    }

    @Test
    void validarHorario_empiezaAntesDeAbrir_lanza409() {
        LocalDateTime inicio = MARTES.atTime(7, 30);
        assertThrows(ReglaNegocioException.class, () -> reglas.validarHorario(horario, inicio, inicio.plusMinutes(45)));
    }

    @Test
    void validarHorario_domingoConSalonCerrado_lanza409() {
        LocalDateTime inicio = DOMINGO.atTime(10, 0);
        assertThrows(ReglaNegocioException.class, () -> reglas.validarHorario(horario, inicio, inicio.plusMinutes(45)));
    }

    @Test
    void validarHorario_domingoConSalonAbierto_noLanza() {
        ConfiguracionSalon abreDomingos = new ConfiguracionSalon(LocalTime.of(8, 0), LocalTime.of(19, 0), true, 30);
        LocalDateTime inicio = DOMINGO.atTime(10, 0);
        assertDoesNotThrow(() -> reglas.validarHorario(abreDomingos, inicio, inicio.plusMinutes(45)));
    }

    @Test
    void validarNoPasado_horaQueYaPaso_lanza409() {
        assertThrows(ReglaNegocioException.class, () -> reglas.validarNoPasado(HOY.atTime(9, 0)));
    }

    @Test
    void validarNoPasado_masTardeHoy_noLanza() {
        assertDoesNotThrow(() -> reglas.validarNoPasado(HOY.atTime(15, 0)));
    }

    @Test
    void horariosLibres_conCitaDeMateoALas10_excluyeLosCruces() {
        // Valentina tiene corte de 10:00 a 10:45; se pide disponibilidad para otro corte de 45 minutos
        List<ReglasAgenda.Intervalo> ocupados = List.of(
                new ReglasAgenda.Intervalo(MARTES.atTime(10, 0), MARTES.atTime(10, 45)));

        List<LocalTime> libres = reglas.horariosLibres(horario, MARTES, 45, ocupados);

        assertFalse(libres.contains(LocalTime.of(9, 30)), "9:30 terminaría a las 10:15, encima de la cita");
        assertFalse(libres.contains(LocalTime.of(10, 0)));
        assertFalse(libres.contains(LocalTime.of(10, 30)));
        assertTrue(libres.contains(LocalTime.of(9, 0)), "9:00 termina a las 9:45, antes de la cita");
        assertTrue(libres.contains(LocalTime.of(11, 0)));
    }

    @Test
    void horariosLibres_elServicioDebeCaberAntesDelCierre() {
        List<LocalTime> libres = reglas.horariosLibres(horario, MARTES, 45, List.of());

        assertTrue(libres.contains(LocalTime.of(8, 0)));
        assertTrue(libres.contains(LocalTime.of(18, 0)), "18:00 + 45 = 18:45, cabe");
        assertFalse(libres.contains(LocalTime.of(18, 30)), "18:30 + 45 = 19:15, no cabe");
    }

    @Test
    void horariosLibres_diaCerrado_noTieneHorarios() {
        assertTrue(reglas.horariosLibres(horario, DOMINGO, 45, List.of()).isEmpty());
    }

    @Test
    void horariosLibres_hoy_noOfreceHorasPasadas() {
        List<LocalTime> libres = reglas.horariosLibres(horario, HOY, 45, List.of());

        assertFalse(libres.contains(LocalTime.of(8, 0)));
        assertFalse(libres.contains(LocalTime.of(9, 0)));
        assertTrue(libres.contains(LocalTime.of(9, 30)), "son las 9:10: la siguiente hora libre es 9:30");
    }
}
