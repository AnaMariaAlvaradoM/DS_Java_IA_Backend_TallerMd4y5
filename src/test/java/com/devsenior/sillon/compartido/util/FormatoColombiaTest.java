package com.devsenior.sillon.compartido.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class FormatoColombiaTest {

    @Test
    void hora_manana_usaAmConEspaciosNormales() {
        assertEquals("10:30 a. m.", FormatoColombia.hora(LocalTime.of(10, 30)));
    }

    @Test
    void hora_tardeYMediodia_usaPm() {
        assertEquals("7:00 p. m.", FormatoColombia.hora(LocalTime.of(19, 0)));
        assertEquals("12:00 p. m.", FormatoColombia.hora(LocalTime.of(12, 0)));
    }

    @Test
    void fecha_enEspanolDeColombia() {
        assertEquals("martes 20 de octubre", FormatoColombia.fecha(LocalDate.of(2026, 10, 20)));
    }

    @Test
    void pesos_conPuntoDeMiles() {
        assertEquals("$35.000", FormatoColombia.pesos(35000));
    }
}
