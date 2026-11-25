package com.devsenior.sillon.ia;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProveedorIaExceptionTest {

    @Test
    @DisplayName("Los errores HTTP 400, 401, 403 y 404 se clasifican como errores de configuración")
    void clasificacionErroresConfiguracion() {
        assertTrue(ProveedorIaException.desdeEstadoHttp("openai", 400).isErrorDeConfiguracion());
        assertTrue(ProveedorIaException.desdeEstadoHttp("openai", 401).isErrorDeConfiguracion());
        assertTrue(ProveedorIaException.desdeEstadoHttp("claude", 403).isErrorDeConfiguracion());
        assertTrue(ProveedorIaException.desdeEstadoHttp("claude", 404).isErrorDeConfiguracion());
    }

    @Test
    @DisplayName("Los errores HTTP 429, 500, 502 y 503 se clasifican como errores temporales")
    void clasificacionErroresTemporales() {
        assertFalse(ProveedorIaException.desdeEstadoHttp("openai", 429).isErrorDeConfiguracion());
        assertFalse(ProveedorIaException.desdeEstadoHttp("openai", 500).isErrorDeConfiguracion());
        assertFalse(ProveedorIaException.desdeEstadoHttp("claude", 502).isErrorDeConfiguracion());
        assertFalse(ProveedorIaException.desdeEstadoHttp("claude", 503).isErrorDeConfiguracion());
    }
}
