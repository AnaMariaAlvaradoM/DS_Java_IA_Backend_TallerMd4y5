
package com.devsenior.sillon.compartido;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ManejadorGlobalExcepcionesTest {

    @Test
    void debeResponder409CuandoExisteConflicto() {

        // Crear un reloj fijo para la prueba
        Clock reloj = Clock.fixed(
                Instant.parse("2026-11-07T15:30:00Z"),
                ZoneId.of("America/Bogota")
        );

        // Crear el manejador global
        ManejadorGlobalExcepciones manejador =
                new ManejadorGlobalExcepciones(reloj);

        // Simular un conflicto de negocio
        ReglaNegocioException excepcion =
                new ReglaNegocioException(
                        "El correo ya está registrado"
                );

        // Ejecutar el manejador
        ResponseEntity<RespuestaError> respuesta =
                manejador.manejarConflicto(excepcion);

        // Verificar el código HTTP
        assertEquals(HttpStatus.CONFLICT,
                respuesta.getStatusCode());

        // Verificar los cuatro campos
        RespuestaError cuerpo = respuesta.getBody();

        assertNotNull(cuerpo);
        assertEquals(409, cuerpo.codigo());
        assertEquals("Conflict", cuerpo.error());
        assertEquals("El correo ya está registrado",
                cuerpo.mensaje());
        assertNotNull(cuerpo.fecha());
    }
}
