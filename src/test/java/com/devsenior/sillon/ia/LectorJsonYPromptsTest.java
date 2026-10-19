package com.devsenior.sillon.ia;

import static org.junit.jupiter.api.Assertions.*;
import com.devsenior.sillon.ia.dto.FacturaIA;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

class LectorJsonYPromptsTest {
    private final LectorJsonIA lector = new LectorJsonIA(JsonMapper.builder().build());
    public record Mensaje(String mensaje) { }

    @Test void leeJsonConBloqueMarkdownYFacturaConLosSeisCampos() {
        RespuestaIA respuesta = respuesta("```json\n{\"mensaje\":\"Hola\"}\n```");
        assertEquals("Hola", lector.leer(respuesta, Mensaje.class).mensaje());
        FacturaIA factura = FacturaIA.de(respuesta);
        assertEquals("openai", factura.proveedor()); assertEquals("simulado", factura.modelo());
        assertEquals(10, factura.tokensEntrada()); assertEquals(5, factura.tokensSalida());
        assertEquals(new BigDecimal("0.0001"), factura.costoEstimadoUsd()); assertEquals(20, factura.milisegundos());
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" ", "no es json", "{mensaje:mal}"})
    void jsonInvalidoEs502YNoRevelaTexto(String texto) {
        var error = assertThrows(RespuestaIAInvalidaException.class, () -> lector.leer(respuesta(texto), Mensaje.class));
        assertEquals(HttpStatus.BAD_GATEWAY, error.getEstado());
        assertEquals("La IA respondió un valor no válido", error.getMessage());
    }

    @Test void cargaPromptDesdeClasspathYSustituyeVariables() {
        assertEquals("Hola Ana, equipo Sillón.", new BibliotecaPrompts()
                .cargar("fase7-prueba", Map.of("cliente", "Ana", "salon", "Sillón")).strip());
        assertThrows(IllegalStateException.class, () -> new BibliotecaPrompts().cargar("fase7-prueba"));
        assertThrows(IllegalStateException.class, () -> new BibliotecaPrompts().cargar("inexistente"));
    }

    private RespuestaIA respuesta(String texto) {
        return new RespuestaIA(texto, "openai", "simulado", 10, 5, new BigDecimal("0.0001"), 20);
    }
}
