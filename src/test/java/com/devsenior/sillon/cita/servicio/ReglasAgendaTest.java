package com.devsenior.sillon.cita.servicio;

import com.devsenior.sillon.cita.modelo.Cita;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.modelo.ConfiguracionSalon;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReglasAgendaTest {

    private final ReglasAgenda reglasAgenda = new ReglasAgenda();
    private final ConfiguracionSalon config = new ConfiguracionSalon(
            LocalTime.of(8, 0), LocalTime.of(19, 0), false, 30
    );

    @Test
    void validarHorario_citaValidaEntreSemana_noLanzaExcepcion() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 24, 10, 0);
        LocalDateTime fin = inicio.plusMinutes(45);
        assertDoesNotThrow(() -> reglasAgenda.validarHorario(inicio, fin, config));
    }

    @Test
    void validarHorario_domingoCerrado_lanzaReglaNegocio() {
        LocalDateTime domingo = LocalDateTime.of(2026, 11, 22, 10, 0);
        assertThrows(ReglaNegocioException.class,
                () -> reglasAgenda.validarHorario(domingo, domingo.plusMinutes(30), config));
    }

    @Test
    void validarHorario_domingoAbierto_noLanzaExcepcion() {
        ConfiguracionSalon abiertoDomingos = new ConfiguracionSalon(
                LocalTime.of(8, 0), LocalTime.of(19, 0), true, 30
        );
        LocalDateTime domingo = LocalDateTime.of(2026, 11, 22, 10, 0);
        assertDoesNotThrow(() -> reglasAgenda.validarHorario(domingo, domingo.plusMinutes(30), abiertoDomingos));
    }

    @Test
    void validarHorario_antesDeApertura_lanzaReglaNegocio() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 24, 7, 30);
        assertThrows(ReglaNegocioException.class,
                () -> reglasAgenda.validarHorario(inicio, inicio.plusMinutes(30), config));
    }

    @Test
    void validarHorario_terminaDespuesDelCierre_lanzaReglaNegocio() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 24, 18, 45);
        assertThrows(ReglaNegocioException.class,
                () -> reglasAgenda.validarHorario(inicio, inicio.plusMinutes(45), config));
    }

    @Test
    void validarHorario_cruzaMedianoche_lanzaReglaNegocio() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 24, 23, 45);
        assertThrows(ReglaNegocioException.class,
                () -> reglasAgenda.validarHorario(inicio, inicio.plusMinutes(45), config));
    }

    @Test
    void seCruzan_intervalosSolapados_retornaTrue() {
        LocalDateTime a1 = LocalDateTime.of(2026, 11, 24, 10, 0);
        LocalDateTime a2 = LocalDateTime.of(2026, 11, 24, 10, 45);
        LocalDateTime b1 = LocalDateTime.of(2026, 11, 24, 10, 30);
        LocalDateTime b2 = LocalDateTime.of(2026, 11, 24, 11, 0);
        assertTrue(reglasAgenda.seCruzan(a1, a2, b1, b2));
    }

    @Test
    void seCruzan_citasContiguas_retornaFalse() {
        LocalDateTime a1 = LocalDateTime.of(2026, 11, 24, 10, 0);
        LocalDateTime a2 = LocalDateTime.of(2026, 11, 24, 10, 45);
        LocalDateTime b1 = LocalDateTime.of(2026, 11, 24, 10, 45);
        LocalDateTime b2 = LocalDateTime.of(2026, 11, 24, 11, 30);
        assertFalse(reglasAgenda.seCruzan(a1, a2, b1, b2));
    }

    @Test
    void calcularHorariosLibres_descartaCruzadosYPasados() {
        LocalDate fecha = LocalDate.of(2026, 11, 24);
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 24, 9, 0);

        Cita ocupada = new Cita();
        ocupada.setInicio(LocalDateTime.of(2026, 11, 24, 10, 0));
        ocupada.setFin(LocalDateTime.of(2026, 11, 24, 10, 45));

        List<LocalTime> libres = reglasAgenda.calcularHorariosLibres(
                fecha, 45, config, List.of(ocupada), ahora
        );

        assertFalse(libres.contains(LocalTime.of(9, 0)));
        assertFalse(libres.contains(LocalTime.of(9, 30)));
        assertFalse(libres.contains(LocalTime.of(10, 0)));
        assertFalse(libres.contains(LocalTime.of(10, 30)));
        assertTrue(libres.contains(LocalTime.of(11, 0)));
    }

    @Test
    void calcularHorariosLibres_domingoCerrado_retornaListaVacia() {
        LocalDate domingo = LocalDate.of(2026, 11, 22);
        List<LocalTime> libres = reglasAgenda.calcularHorariosLibres(
                domingo, 30, config, List.of(), LocalDateTime.of(2026, 11, 20, 10, 0)
        );
        assertTrue(libres.isEmpty());
    }
}

