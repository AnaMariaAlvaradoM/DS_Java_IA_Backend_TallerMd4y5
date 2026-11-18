package com.devsenior.sillon.ia.util;

import com.devsenior.sillon.chatbot.dto.RespuestaChatbotIA;
import com.devsenior.sillon.compartido.RespuestaIAInvalidaException;
import com.devsenior.sillon.ia.RespuestaIA;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LectorJsonIaTest {
    private LectorJsonIA lectorJsonIA;

    @BeforeEach
    void prepararLector() {
        lectorJsonIA = new LectorJsonIA(JsonMapper.builder().build());
    }

    @Test
    void parseaJsonLimpio() {
        RespuestaChatbotIA respuesta = lectorJsonIA.leer(
                respuesta("{\"respuesta\":\"Hola\",\"escalar\":false}"),
                RespuestaChatbotIA.class);

        assertEquals("Hola", respuesta.respuesta());
        assertEquals(false, respuesta.escalar());
    }

    @Test
    void parseaJsonDentroDeBloqueMarkdown() {
        RespuestaChatbotIA respuesta = lectorJsonIA.leer(
                respuesta("```json\n{\"respuesta\":\"Hola\",\"escalar\":true}\n```"),
                RespuestaChatbotIA.class);

        assertEquals("Hola", respuesta.respuesta());
        assertEquals(true, respuesta.escalar());
    }

    @Test
    void jsonMalformadoLanzaExcepcionDeRespuestaInvalida() {
        assertThrows(RespuestaIAInvalidaException.class,
                () -> lectorJsonIA.leer(respuesta("{\"respuesta\": }"), RespuestaChatbotIA.class));
    }

    private RespuestaIA respuesta(String texto) {
        return new RespuestaIA(texto, "mock", "modelo-test", 1, 1, BigDecimal.ZERO, 1L);
    }
}
