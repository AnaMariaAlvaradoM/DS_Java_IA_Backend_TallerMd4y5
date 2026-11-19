package com.devsenior.sillon.ia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.devsenior.sillon.ia.excepcion.RespuestaIAInvalidaException;
import com.devsenior.sillon.ia.model.RespuestaIA;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class LectorJsonIATest {

    public record Saludo(String saludo, Boolean escalar) {
    }

    private final LectorJsonIA lector = new LectorJsonIA(JsonMapper.builder().build());

    @Test
    void leer_jsonLimpio_loConvierte() {
        assertEquals("hola", lector.leer(respuesta("{\"saludo\":\"hola\",\"escalar\":false}"), Saludo.class).saludo());
    }

    @Test
    void leer_jsonEnvueltoEnBloqueDeCodigo_loConvierte() {
        String texto = "```json\n{\"saludo\":\"hola\",\"escalar\":true}\n```";
        assertEquals(true, lector.leer(respuesta(texto), Saludo.class).escalar());
    }

    @Test
    void leer_textoAntesDelJson_loConvierte() {
        assertEquals("hola", lector.leer(respuesta("Claro, aquí está: {\"saludo\":\"hola\"}"), Saludo.class).saludo());
    }

    @Test
    void leer_sinJson_lanza502() {
        assertThrows(RespuestaIAInvalidaException.class, () -> lector.leer(respuesta("Hola, ¿en qué le ayudo?"), Saludo.class));
    }

    @Test
    void leer_jsonCortado_lanza502() {
        assertThrows(RespuestaIAInvalidaException.class, () -> lector.leer(respuesta("{\"saludo\":\"ho"), Saludo.class));
    }

    @Test
    void leer_jsonMalFormado_lanza502() {
        assertThrows(RespuestaIAInvalidaException.class, () -> lector.leer(respuesta("{\"saludo\": hola}"), Saludo.class));
    }

    private RespuestaIA respuesta(String texto) {
        return new RespuestaIA(texto, "openai", "modelo", 10, 5, BigDecimal.ZERO, 1);
    }
}
