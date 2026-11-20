package com.devsenior.sillon.compartido;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class ManejadorErroresTest {

    @Test
    void respondeConflictoConFormatoUnicoYHoraDelNegocio() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneId.of("America/Santiago"));
        ManejadorErrores manejador = new ManejadorErrores(reloj);

        var respuesta = manejador.manejarNegocio(new ReglaNegocioException("La cita se cruza con otra"));

        assertEquals(409, respuesta.getStatusCode().value());
        assertEquals(409, respuesta.getBody().codigo());
        assertEquals("Conflict", respuesta.getBody().error());
        assertEquals("La cita se cruza con otra", respuesta.getBody().mensaje());
        assertEquals(12, respuesta.getBody().fecha().getHour());
    }
}
