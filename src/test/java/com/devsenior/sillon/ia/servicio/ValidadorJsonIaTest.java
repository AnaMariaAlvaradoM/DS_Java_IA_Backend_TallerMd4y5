
package com.devsenior.sillon.ia.servicio;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devsenior.sillon.compartido.excepcion.ExcepcionIntegracion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class ValidadorJsonIaTest {

    private ValidadorJsonIa validador;

    @BeforeEach
    void preparar() {
        validador = new ValidadorJsonIa(new ObjectMapper());
    }

    @Test
    void interpretaRecordatorioValido() {

        String contenido = """
                {
                  "mensaje": "Hola Sara, su cita es a las 10:30."
                }
                """;

        JsonNode respuesta = validador.interpretar(contenido);

        assertEquals(
                "Hola Sara, su cita es a las 10:30.",
                validador.obtenerTexto(respuesta, "mensaje"));
    }

    @Test
    void interpretaChatbotSinEscalamiento() {

        String contenido = """
                {
                  "respuesta": "El corte dura 45 minutos.",
                  "escalar": false
                }
                """;

        JsonNode respuesta = validador.interpretar(contenido);

        assertEquals(
                "El corte dura 45 minutos.",
                validador.obtenerTexto(respuesta, "respuesta"));

        assertFalse(
                validador.obtenerBooleano(respuesta, "escalar"));
    }

    @Test
    void interpretaChatbotConEscalamiento() {

        String contenido = """
                {
                  "respuesta": "Una persona lo contactará.",
                  "escalar": true
                }
                """;

        JsonNode respuesta = validador.interpretar(contenido);

        assertTrue(
                validador.obtenerBooleano(respuesta, "escalar"));
    }

    @Test
    void rechazaContenidoNulo() {

        assertThrows(
                ExcepcionIntegracion.class,
                () -> validador.interpretar(null));
    }

    @Test
    void rechazaContenidoVacio() {

        assertThrows(
                ExcepcionIntegracion.class,
                () -> validador.interpretar("   "));
    }

    @Test
    void rechazaJsonMalFormado() {

        String contenido = """
                {
                  "mensaje":
                }
                """;

        assertThrows(
                ExcepcionIntegracion.class,
                () -> validador.interpretar(contenido));
    }

    @Test
    void rechazaJsonQueNoEsObjeto() {

        assertThrows(
                ExcepcionIntegracion.class,
                () -> validador.interpretar("[1, 2, 3]"));
    }

    @Test
    void rechazaCampoTextoAusente() {

        JsonNode respuesta = validador.interpretar(
                """
                {
                  "otroCampo": "valor"
                }
                """);

        assertThrows(
                ExcepcionIntegracion.class,
                () -> validador.obtenerTexto(
                        respuesta, "mensaje"));
    }

    @Test
    void rechazaCampoTextoVacio() {

        JsonNode respuesta = validador.interpretar(
                """
                {
                  "mensaje": ""
                }
                """);

        assertThrows(
                ExcepcionIntegracion.class,
                () -> validador.obtenerTexto(
                        respuesta, "mensaje"));
    }

    @Test
    void rechazaCampoBooleanoConTipoIncorrecto() {

        JsonNode respuesta = validador.interpretar(
                """
                {
                  "escalar": "true"
                }
                """);

        assertThrows(
                ExcepcionIntegracion.class,
                () -> validador.obtenerBooleano(
                        respuesta, "escalar"));
    }

    @Test
    void rechazaCampoBooleanoAusente() {

        JsonNode respuesta = validador.interpretar(
                """
                {
                  "respuesta": "Hola"
                }
                """);

        assertThrows(
                ExcepcionIntegracion.class,
                () -> validador.obtenerBooleano(
                        respuesta, "escalar"));
    }
}
