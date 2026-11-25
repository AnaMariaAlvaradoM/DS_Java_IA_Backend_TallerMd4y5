package com.devsenior.sillon.ia;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.devsenior.sillon.compartido.PasarelaException;

class BibliotecaPromptsTest {

    private final BibliotecaPrompts biblioteca = new BibliotecaPrompts();

    @Test
    @DisplayName("Carga plantilla recordatorio.txt y reemplaza correctamente las variables")
    void obtenerPrompt_recordatorio_reemplazaVariables() {
        Map<String, String> vars = Map.of(
                "salon", "Barbería La Navaja",
                "cliente", "Camilo",
                "datos", "Servicio: Corte clásico"
        );

        String prompt = biblioteca.obtenerPrompt("recordatorio", vars);

        assertTrue(prompt.contains("Barbería La Navaja"));
        assertTrue(prompt.contains("Camilo"));
        assertTrue(prompt.contains("Servicio: Corte clásico"));
    }

    @Test
    @DisplayName("Carga plantilla chatbot.txt y reemplaza catálogo y horario")
    void obtenerPrompt_chatbot_reemplazaVariables() {
        Map<String, String> vars = Map.of(
                "salon", "Barbería La Navaja",
                "direccion", "Calle 10",
                "horario", "8 a 19",
                "catalogo", "- Corte: $35.000",
                "cliente", "Juliana"
        );

        String prompt = biblioteca.obtenerPrompt("chatbot", vars);

        assertTrue(prompt.contains("Barbería La Navaja"));
        assertTrue(prompt.contains("- Corte: $35.000"));
        assertTrue(prompt.contains("Juliana"));
    }

    @Test
    @DisplayName("Solicitar plantilla inexistente lanza PasarelaException (502)")
    void plantillaInexistente_lanzaExcepcion() {
        assertThrows(PasarelaException.class,
                () -> biblioteca.obtenerPrompt("inexistente_xyz", Map.of()));
    }
}
