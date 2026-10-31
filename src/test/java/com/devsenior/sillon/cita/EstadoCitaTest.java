package com.devsenior.sillon.cita;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class EstadoCitaTest {

    static Stream<Arguments> transicionesPermitidas() {
        return Stream.of(
                Arguments.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA),
                Arguments.of(EstadoCita.PENDIENTE, EstadoCita.CANCELADA),
                Arguments.of(EstadoCita.CONFIRMADA, EstadoCita.COMPLETADA),
                Arguments.of(EstadoCita.CONFIRMADA, EstadoCita.CANCELADA),
                Arguments.of(EstadoCita.CONFIRMADA, EstadoCita.NO_ASISTIO));
    }

    @ParameterizedTest
    @MethodSource("transicionesPermitidas")
    void permiteLasTransicionesValidas(EstadoCita origen, EstadoCita destino) {
        assertThat(origen.puedeCambiarA(destino)).isTrue();
    }

    static Stream<Arguments> transicionesBloqueadas() {
        return Stream.of(
                Arguments.of(EstadoCita.PENDIENTE, EstadoCita.COMPLETADA),
                Arguments.of(EstadoCita.PENDIENTE, EstadoCita.NO_ASISTIO),
                Arguments.of(EstadoCita.PENDIENTE, EstadoCita.PENDIENTE),
                Arguments.of(EstadoCita.CONFIRMADA, EstadoCita.PENDIENTE),
                Arguments.of(EstadoCita.CONFIRMADA, EstadoCita.CONFIRMADA),
                Arguments.of(EstadoCita.COMPLETADA, EstadoCita.CONFIRMADA),
                Arguments.of(EstadoCita.COMPLETADA, EstadoCita.PENDIENTE),
                Arguments.of(EstadoCita.CANCELADA, EstadoCita.CONFIRMADA),
                Arguments.of(EstadoCita.CANCELADA, EstadoCita.PENDIENTE),
                Arguments.of(EstadoCita.NO_ASISTIO, EstadoCita.CONFIRMADA),
                Arguments.of(EstadoCita.NO_ASISTIO, EstadoCita.PENDIENTE));
    }

    @ParameterizedTest
    @MethodSource("transicionesBloqueadas")
    void bloqueaLasTransicionesInvalidas(EstadoCita origen, EstadoCita destino) {
        assertThat(origen.puedeCambiarA(destino)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCita.class, names = { "PENDIENTE", "CONFIRMADA" })
    void ocupaAgendaSoloPendienteYConfirmada(EstadoCita estado) {
        assertThat(estado.ocupaAgenda()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCita.class, names = { "COMPLETADA", "CANCELADA", "NO_ASISTIO" })
    void noOcupaAgendaLosEstadosFinales(EstadoCita estado) {
        assertThat(estado.ocupaAgenda()).isFalse();
    }
}
