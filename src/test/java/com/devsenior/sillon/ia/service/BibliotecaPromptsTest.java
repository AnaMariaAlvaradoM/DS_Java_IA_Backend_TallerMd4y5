package com.devsenior.sillon.ia.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Usa src/test/resources/prompts/prueba-biblioteca.txt. */
class BibliotecaPromptsTest {

    private final BibliotecaPrompts biblioteca = new BibliotecaPrompts();

    @Test
    void cargar_conTodasLasVariables_lasReemplaza() {
        String prompt = biblioteca.cargar("prueba-biblioteca", Map.of("cliente", "Valentina", "salon", "La Navaja"));

        assertTrue(prompt.contains("Hola Valentina, bienvenido a La Navaja."));
    }

    @Test
    void cargar_conUnaVariableFaltante_fallaAntesDeLlamarALaIA() {
        assertThrows(IllegalStateException.class,
                () -> biblioteca.cargar("prueba-biblioteca", Map.of("cliente", "Valentina")));
    }

    @Test
    void cargar_promptInexistente_falla() {
        assertThrows(IllegalStateException.class, () -> biblioteca.cargar("no-existe"));
    }
}
