package com.devsenior.sillon.cita;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EstadoCitaTest {

    @ParameterizedTest(name = "{0} -> {1} debe ser permitida")
    @CsvSource({
            "PENDIENTE, CONFIRMADA",
            "PENDIENTE, CANCELADA",
            "CONFIRMADA, COMPLETADA",
            "CONFIRMADA, CANCELADA",
            "CONFIRMADA, NO_ASISTIO"
    })
    @DisplayName("5 transiciones permitidas según el contrato")
    void transicionesPermitidas(EstadoCita origen, EstadoCita destino) {
        assertTrue(origen.puedeTransicionarA(destino));
    }

    @ParameterizedTest(name = "{0} -> {1} debe ser bloqueada")
    @CsvSource({
            "CONFIRMADA, PENDIENTE",
            "COMPLETADA, CANCELADA",
            "COMPLETADA, PENDIENTE",
            "CANCELADA, CONFIRMADA",
            "CANCELADA, PENDIENTE",
            "NO_ASISTIO, COMPLETADA",
            "NO_ASISTIO, PENDIENTE",
            "PENDIENTE, COMPLETADA",
            "PENDIENTE, NO_ASISTIO"
    })
    @DisplayName("Transiciones bloqueadas (estados finales o regresiones)")
    void transicionesBloqueadas(EstadoCita origen, EstadoCita destino) {
        assertFalse(origen.puedeTransicionarA(destino));
    }

    @Test
    @DisplayName("Solo PENDIENTE y CONFIRMADA ocupan agenda")
    void ocupaAgenda_soloPendienteYConfirmada() {
        assertTrue(EstadoCita.PENDIENTE.ocupaAgenda());
        assertTrue(EstadoCita.CONFIRMADA.ocupaAgenda());
        assertFalse(EstadoCita.COMPLETADA.ocupaAgenda());
        assertFalse(EstadoCita.CANCELADA.ocupaAgenda());
        assertFalse(EstadoCita.NO_ASISTIO.ocupaAgenda());
    }
}
