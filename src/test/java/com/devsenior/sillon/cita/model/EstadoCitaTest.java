package com.devsenior.sillon.cita.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/** La tabla de transiciones de la SPEC convertida en datos de prueba. */
class EstadoCitaTest {

    @ParameterizedTest(name = "{0} → {1} está permitida")
    @CsvSource({
            "PENDIENTE, CONFIRMADA",
            "PENDIENTE, CANCELADA",
            "CONFIRMADA, COMPLETADA",
            "CONFIRMADA, CANCELADA",
            "CONFIRMADA, NO_ASISTIO"
    })
    void puedeCambiarA_transicionPermitida_devuelveTrue(EstadoCita desde, EstadoCita hacia) {
        assertTrue(desde.puedeCambiarA(hacia));
    }

    @ParameterizedTest(name = "{0} → {1} está bloqueada")
    @CsvSource({
            "CONFIRMADA, PENDIENTE",
            "PENDIENTE, COMPLETADA",
            "PENDIENTE, NO_ASISTIO",
            "COMPLETADA, CANCELADA",
            "CANCELADA, CONFIRMADA",
            "NO_ASISTIO, COMPLETADA",
            "COMPLETADA, PENDIENTE",
            "PENDIENTE, PENDIENTE"
    })
    void puedeCambiarA_transicionBloqueada_devuelveFalse(EstadoCita desde, EstadoCita hacia) {
        assertFalse(desde.puedeCambiarA(hacia));
    }

    @ParameterizedTest(name = "{0} ocupa agenda")
    @EnumSource(value = EstadoCita.class, names = {"PENDIENTE", "CONFIRMADA"})
    void ocupaAgenda_pendienteYConfirmada_devuelveTrue(EstadoCita estado) {
        assertTrue(estado.ocupaAgenda());
    }

    @ParameterizedTest(name = "{0} no ocupa agenda")
    @EnumSource(value = EstadoCita.class, names = {"COMPLETADA", "CANCELADA", "NO_ASISTIO"})
    void ocupaAgenda_estadosFinales_devuelveFalse(EstadoCita estado) {
        assertFalse(estado.ocupaAgenda());
    }
}
