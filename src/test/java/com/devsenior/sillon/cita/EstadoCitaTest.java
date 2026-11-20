package com.devsenior.sillon.cita;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class EstadoCitaTest {
    static Stream<Arguments> transiciones() {
        return Stream.of(
                Arguments.of(EstadoCita.PENDIENTE, EstadoCita.CONFIRMADA, true),
                Arguments.of(EstadoCita.PENDIENTE, EstadoCita.CANCELADA, true),
                Arguments.of(EstadoCita.CONFIRMADA, EstadoCita.COMPLETADA, true),
                Arguments.of(EstadoCita.CONFIRMADA, EstadoCita.CANCELADA, true),
                Arguments.of(EstadoCita.CONFIRMADA, EstadoCita.NO_ASISTIO, true),
                Arguments.of(EstadoCita.PENDIENTE, EstadoCita.COMPLETADA, false),
                Arguments.of(EstadoCita.PENDIENTE, EstadoCita.NO_ASISTIO, false),
                Arguments.of(EstadoCita.CONFIRMADA, EstadoCita.PENDIENTE, false),
                Arguments.of(EstadoCita.COMPLETADA, EstadoCita.CANCELADA, false),
                Arguments.of(EstadoCita.CANCELADA, EstadoCita.CONFIRMADA, false),
                Arguments.of(EstadoCita.NO_ASISTIO, EstadoCita.COMPLETADA, false));
    }

    @ParameterizedTest
    @MethodSource("transiciones")
    void aplicaMaquinaDeEstados(EstadoCita actual, EstadoCita destino, boolean permitido) {
        assertEquals(permitido, actual.puedeTransicionarA(destino));
    }
}
