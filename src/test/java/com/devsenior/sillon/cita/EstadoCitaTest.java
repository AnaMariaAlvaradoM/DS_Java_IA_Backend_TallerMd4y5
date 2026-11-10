package com.devsenior.sillon.cita;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadoCitaTest {

    @Test
    @DisplayName("1. Verificar existencia del estado CONFIRMADA")
    void estadoConfirmadaExiste() {
        assertNotNull(EstadoCita.valueOf("CONFIRMADA"));
    }

    @Test
    @DisplayName("2. Verificar existencia del estado CANCELADA")
    void estadoCanceladaExiste() {
        assertNotNull(EstadoCita.valueOf("CANCELADA"));
    }

    @Test
    @DisplayName("3. Los valores del enum deben contener estados principales")
    void enumContieneValoresEsperados() {
        EstadoCita[] estados = EstadoCita.values();
        assertTrue(estados.length >= 2, "El enum debe tener al menos 2 estados definidos");
    }

    @Test
    @DisplayName("4. El nombre del estado CONFIRMADA debe coincidir")
    void nombreConfirmadaCoincide() {
        assertTrue(EstadoCita.CONFIRMADA.name().equals("CONFIRMADA"));
    }

    @Test
    @DisplayName("5. El nombre del estado CANCELADA debe coincidir")
    void nombreCanceladaCoincide() {
        assertTrue(EstadoCita.CANCELADA.name().equals("CANCELADA"));
    }

    @Test
    @DisplayName("6. Conversion de String a Enum es consistente")
    void conversionStringAEnum() {
        EstadoCita estado = EstadoCita.valueOf(EstadoCita.CONFIRMADA.name());
        assertTrue(estado == EstadoCita.CONFIRMADA);
    }
}
