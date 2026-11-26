package com.devsenior.sillon.cita;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EstadoCitaTest {

    @ParameterizedTest(name = "{0} → {1} está permitida")
    @CsvSource({
            "PENDIENTE, CONFIRMADA",
            "PENDIENTE, CANCELADA",
            "CONFIRMADA, COMPLETADA",
            "CONFIRMADA, CANCELADA",
            "CONFIRMADA, NO_ASISTIO" })
    void transicionesPermitidas(EstadoCita desde, EstadoCita hacia) {
        assertThat(desde.puedeCambiarA(hacia)).isTrue();
    }

    @ParameterizedTest(name = "{0} → {1} está bloqueada")
    @CsvSource({
            "CONFIRMADA, PENDIENTE",
            "PENDIENTE, COMPLETADA",
            "PENDIENTE, NO_ASISTIO",
            "COMPLETADA, CANCELADA",
            "CANCELADA, CONFIRMADA",
            "NO_ASISTIO, COMPLETADA",
            "COMPLETADA, PENDIENTE" })
    void transicionesBloqueadas(EstadoCita desde, EstadoCita hacia) {
        assertThat(desde.puedeCambiarA(hacia)).isFalse();
    }

    @Test
    void soloPendienteYConfirmadaOcupanAgenda() {
        assertThat(EstadoCita.PENDIENTE.ocupaAgenda()).isTrue();
        assertThat(EstadoCita.CONFIRMADA.ocupaAgenda()).isTrue();
        assertThat(EstadoCita.CANCELADA.ocupaAgenda()).isFalse();
        assertThat(EstadoCita.COMPLETADA.ocupaAgenda()).isFalse();
        assertThat(EstadoCita.NO_ASISTIO.ocupaAgenda()).isFalse();
    }
}
