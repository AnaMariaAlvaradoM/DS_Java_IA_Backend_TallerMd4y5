package com.devsenior.sillon.cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class EstadoCitaTest {
    static Stream<Arguments> transiciones() {
        Set<String> permitidas = Set.of("PENDIENTE/CONFIRMADA", "PENDIENTE/CANCELADA", "CONFIRMADA/COMPLETADA",
                "CONFIRMADA/CANCELADA", "CONFIRMADA/NO_ASISTIO");
        return Stream.of(EstadoCita.values()).flatMap(origen -> Stream.of(EstadoCita.values())
                .map(destino -> Arguments.of(origen, destino, permitidas.contains(origen + "/" + destino))));
    }

    @ParameterizedTest @MethodSource("transiciones")
    void respetaLaTablaCompleta(EstadoCita origen, EstadoCita destino, boolean permitida) {
        assertEquals(permitida, origen.permite(destino));
    }

}
