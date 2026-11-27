package com.devsenior.sillon.ia.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BibliotecaPromptsTest {
    private final BibliotecaPrompts bibliotecaPrompts = new BibliotecaPrompts();

    @Test
    void cargaPromptDeRecordatorioConSusVariablesReemplazadas() {
        String prompt = bibliotecaPrompts.cargar("recordatorio", Map.of(
                "salon", "Sillón Centro",
                "cliente", "Laura",
                "estilista", "Sara",
                "datos", "Servicio: Corte\nHora: 10:00"
        ));

        assertTrue(prompt.contains("SALÓN: Sillón Centro"));
        assertTrue(prompt.contains("CLIENTE: Laura"));
        assertTrue(prompt.contains("ESTILISTA: Sara"));
        assertTrue(prompt.contains("nombre del estilista"));
        assertTrue(prompt.contains("Servicio: Corte\nHora: 10:00"));
        assertFalse(prompt.contains("{{"));
    }

    @Test
    void rechazaVariablesDePromptSinReemplazar() {
        IllegalStateException error = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> bibliotecaPrompts.cargar("recordatorio", Map.of("salon", "Sillón Centro")));

        assertTrue(error.getMessage().contains("variables sin reemplazar"));
    }
}
