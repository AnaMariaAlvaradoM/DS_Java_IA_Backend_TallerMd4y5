package com.devsenior.sillon.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

class BibliotecaPromptsTest {

    private final BibliotecaPrompts bibliotecaPrompts = new BibliotecaPrompts();

    @Test
    void sustituyeTodasLasVariablesDeUnPromptReal() {
        String prompt = bibliotecaPrompts.cargar("chatbot", Map.of(
                "salon", "Salon Bonito",
                "direccion", "Calle 1",
                "horario", "8:00-19:00",
                "catalogo", "- Corte: $20000",
                "cliente", "Ana"));

        assertThat(prompt).contains("Salon Bonito", "Calle 1", "8:00-19:00", "- Corte: $20000", "Ana");
        assertThat(prompt).doesNotContain("{{");
    }

    @Test
    void unaVariableSinReemplazarLanzaExcepcion() {
        assertThatThrownBy(() -> bibliotecaPrompts.cargar("chatbot", Map.of(
                "salon", "Salon Bonito",
                "direccion", "Calle 1",
                "horario", "8:00-19:00",
                "catalogo", "- Corte: $20000")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unPromptInexistenteLanzaExcepcion() {
        assertThatThrownBy(() -> bibliotecaPrompts.cargar("no-existe"))
                .isInstanceOf(IllegalStateException.class);
    }
}
