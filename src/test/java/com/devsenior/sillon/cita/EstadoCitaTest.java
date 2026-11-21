
package com.devsenior.sillon.cita;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class EstadoCitaTest {

    @ParameterizedTest
    @CsvSource({
            "PENDIENTE, CONFIRMADA",
            "PENDIENTE, CANCELADA",
            "CONFIRMADA, COMPLETADA",
            "CONFIRMADA, CANCELADA",
            "CONFIRMADA, NO_ASISTIO"
    })
    void permitirTransicionesValidas(
            EstadoCita actual,
            EstadoCita siguiente
    ) {
        assertTrue(actual.puedeCambiarA(siguiente));
    }

    @ParameterizedTest
    @CsvSource({
            "PENDIENTE, COMPLETADA",
            "PENDIENTE, NO_ASISTIO",
            "PENDIENTE, PENDIENTE",
            "CONFIRMADA, PENDIENTE",
            "CONFIRMADA, CONFIRMADA",
            "COMPLETADA, CONFIRMADA",
            "CANCELADA, PENDIENTE",
            "NO_ASISTIO, CONFIRMADA"
    })
    void rechazarTransicionesInvalidas(
            EstadoCita actual,
            EstadoCita siguiente
    ) {
        assertFalse(actual.puedeCambiarA(siguiente));
    }
}
