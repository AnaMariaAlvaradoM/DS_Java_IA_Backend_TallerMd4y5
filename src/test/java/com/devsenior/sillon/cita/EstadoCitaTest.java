package com.devsenior.sillon.cita;

import static com.devsenior.sillon.cita.EstadoCita.CANCELADA;
import static com.devsenior.sillon.cita.EstadoCita.COMPLETADA;
import static com.devsenior.sillon.cita.EstadoCita.CONFIRMADA;
import static com.devsenior.sillon.cita.EstadoCita.NO_ASISTIO;
import static com.devsenior.sillon.cita.EstadoCita.PENDIENTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class EstadoCitaTest {

    @ParameterizedTest
    @MethodSource("transicionesDelContrato")
    void soloPermiteLosDestinosDefinidosParaCadaEstado(EstadoCita origen, Set<EstadoCita> destinos) {
        assertThat(Arrays.stream(EstadoCita.values()).filter(origen::puedeCambiarA))
                .containsExactlyInAnyOrderElementsOf(destinos);
    }

    private static Stream<Arguments> transicionesDelContrato() {
        return Stream.of(
                arguments(PENDIENTE, Set.of(CONFIRMADA, CANCELADA)),
                arguments(CONFIRMADA, Set.of(COMPLETADA, CANCELADA, NO_ASISTIO)),
                arguments(COMPLETADA, Set.of()),
                arguments(CANCELADA, Set.of()),
                arguments(NO_ASISTIO, Set.of()));
    }

    @ParameterizedTest
    @CsvSource({
            "PENDIENTE, true",
            "CONFIRMADA, true",
            "COMPLETADA, false",
            "CANCELADA, false",
            "NO_ASISTIO, false"
    })
    void soloLasCitasPendientesYConfirmadasOcupanAgenda(EstadoCita estado, boolean ocupaAgenda) {
        assertThat(estado.ocupaAgenda()).isEqualTo(ocupaAgenda);
    }
}
