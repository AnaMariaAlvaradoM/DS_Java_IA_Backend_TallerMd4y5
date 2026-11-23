
package com.devsenior.sillon.cita;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.ConfiguracionSalon;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

class ReglasAgendaTest {

    private final ReglasAgenda reglas = new ReglasAgenda();

    private final ConfiguracionSalon configuracion =
            ConfiguracionSalon.porDefecto();

    @Test
    void permitirCitaDentroDelHorario() {

        LocalDateTime inicio =
                LocalDateTime.of(2026, 11, 17, 10, 0);

        LocalDateTime fin = inicio.plusMinutes(45);

        assertDoesNotThrow(() ->
                reglas.validarHorario(inicio, fin, configuracion)
        );
    }

    @Test
    void rechazarCitaFueraDelHorario() {

        LocalDateTime inicio =
                LocalDateTime.of(2026, 11, 17, 18, 45);

        LocalDateTime fin = inicio.plusMinutes(45);

        assertThrows(ReglaNegocioException.class, () ->
                reglas.validarHorario(inicio, fin, configuracion)
        );
    }

    @Test
    void rechazarCitaEnDomingo() {

        LocalDateTime inicio =
                LocalDateTime.of(2026, 11, 22, 10, 0);

        LocalDateTime fin = inicio.plusMinutes(45);

        assertThrows(ReglaNegocioException.class, () ->
                reglas.validarHorario(inicio, fin, configuracion)
        );
    }

    @Test
    void detectarCruceDeCitas() {

        LocalDateTime inicio =
                LocalDateTime.of(2026, 11, 17, 10, 0);

        assertTrue(reglas.seCruzan(
                inicio,
                inicio.plusMinutes(45),
                inicio.plusMinutes(30),
                inicio.plusMinutes(60)
        ));
    }

    @Test
    void permitirCitasConsecutivas() {

        LocalDateTime inicio =
                LocalDateTime.of(2026, 11, 17, 10, 0);

        assertFalse(reglas.seCruzan(
                inicio,
                inicio.plusMinutes(45),
                inicio.plusMinutes(45),
                inicio.plusMinutes(75)
        ));
    }


    @Test
    void excluirHorariosOcupados() {

        LocalDate fecha = LocalDate.of(2026, 11, 17);

        Clock reloj = Clock.fixed(
                Instant.parse("2026-11-16T17:00:00Z"),
                ZoneId.of("America/Bogota")
        );

        Cita cita = new Cita();
        cita.setInicio(fecha.atTime(10, 0));
        cita.setFin(fecha.atTime(10, 45));
        cita.setEstado(EstadoCita.CONFIRMADA);

        List<LocalTime> libres = reglas.calcularHorariosLibres(
                fecha, 45, configuracion, List.of(cita), reloj
        );

        assertFalse(libres.contains(LocalTime.of(9, 30)));
        assertFalse(libres.contains(LocalTime.of(10, 0)));
        assertFalse(libres.contains(LocalTime.of(10, 30)));

        assertTrue(libres.contains(LocalTime.of(11, 0)));
    }

    @Test
    void citaCanceladaNoBloqueaHorario() {

        LocalDate fecha = LocalDate.of(2026, 11, 17);

        Clock reloj = Clock.fixed(
                Instant.parse("2026-11-16T17:00:00Z"),
                ZoneId.of("America/Bogota")
        );

        Cita cita = new Cita();
        cita.setInicio(fecha.atTime(10, 0));
        cita.setFin(fecha.atTime(10, 45));
        cita.setEstado(EstadoCita.CANCELADA);

        List<LocalTime> libres = reglas.calcularHorariosLibres(
                fecha, 45, configuracion, List.of(cita), reloj
        );

        assertTrue(libres.contains(LocalTime.of(10, 0)));
    }


        @Test
        void rechazarCitaAntesDeLaApertura() {

                LocalDateTime inicio =
            LocalDateTime.of(2026, 11, 17, 7, 30);

                        LocalDateTime fin = inicio.plusMinutes(45);

            assertThrows(ReglaNegocioException.class, () ->
                reglas.validarHorario(
                        inicio,
                        fin,
                        configuracion
                )
            );
        }


        @Test
        void domingoCerradoNoTieneHorariosDisponibles() {

                LocalDate domingo =
                        LocalDate.of(2026, 11, 22);

                Clock reloj = Clock.fixed(
                        Instant.parse("2026-11-21T17:00:00Z"),
                        ZoneId.of("America/Bogota")
                );

                List<LocalTime> horariosLibres =
                        reglas.calcularHorariosLibres(
                                domingo,
                    45,
                                configuracion,
                                List.of(),
                                reloj
                        );

                assertTrue(horariosLibres.isEmpty());
        }


        @Test
        void hoyNoDebeOfrecerHorariosPasados() {

                LocalDate hoy = LocalDate.of(2026, 11, 17);

                Clock reloj = Clock.fixed(
                        Instant.parse("2026-11-17T15:15:00Z"),
                        ZoneId.of("America/Bogota")
                );

                List<LocalTime> horariosLibres =
                        reglas.calcularHorariosLibres(
                                hoy,
                    45,
                                configuracion,
                                List.of(),
                                reloj
                        );

                assertFalse(
                        horariosLibres.contains(LocalTime.of(9, 0))
                );

                assertFalse(
                        horariosLibres.contains(LocalTime.of(10, 0))
                );

                assertTrue(
                        horariosLibres.contains(LocalTime.of(10, 30))
                );

                assertTrue(
                        horariosLibres.contains(LocalTime.of(11, 0))
                );
        }
}
