package com.devsenior.sillon.cita.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadoCitaTest {

    @ParameterizedTest
    @CsvSource({
            "PENDIENTE, CONFIRMADA",
            "PENDIENTE, CANCELADA",
            "CONFIRMADA, COMPLETADA",
            "CONFIRMADA, NO_ASISTIO",
            "CONFIRMADA, CANCELADA"
    })
    void permiteLasCincoTransicionesValidas(EstadoCita actual, EstadoCita siguiente) {
        assertTrue(actual.puedeCambiarA(siguiente));
    }

    @ParameterizedTest
    @CsvSource({
            "PENDIENTE, COMPLETADA",
            "PENDIENTE, NO_ASISTIO",
            "CONFIRMADA, PENDIENTE",
            "COMPLETADA, CONFIRMADA",
            "CANCELADA, PENDIENTE",
            "NO_ASISTIO, CANCELADA"
    })
    void bloqueaTransicionesNoPermitidas(EstadoCita actual, EstadoCita siguiente) {
        assertFalse(actual.puedeCambiarA(siguiente));
    }
}
