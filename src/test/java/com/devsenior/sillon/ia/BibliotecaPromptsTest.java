
package com.devsenior.sillon.ia;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.junit.jupiter.api.Assertions.*;

class BibliotecaPromptsTest {

    private final BibliotecaPrompts biblioteca =
            new BibliotecaPrompts(
                    new DefaultResourceLoader()
            );

    // PRUEBA 1
    // El prompt del chatbot debe cargar
    // y reemplazar los datos del salón.

    @Test
    void cargarPromptChatbotConDatosReales() {

        String resultado = biblioteca.cargar(
                "chatbot",
                Map.of(
                        "salon", "Barbería La Navaja",
                        "direccion", "Calle 10 #20-30",
                        "horario", "Lunes a sábado",
                        "catalogo", "Corte clásico $40.000",
                        "cliente", "Carlos Pérez"
                )
        );

        assertNotNull(resultado);

        assertTrue(
                resultado.contains("Barbería La Navaja")
        );

        assertTrue(
                resultado.contains("Carlos Pérez")
        );

        assertTrue(
                resultado.contains("Corte clásico $40.000")
        );

        assertFalse(resultado.contains("{{salon}}"));
        assertFalse(resultado.contains("{{cliente}}"));
        assertFalse(resultado.contains("{{catalogo}}"));
    }

    // PRUEBA 2
    // El prompt del recordatorio debe
    // incorporar los datos de la cita.

    @Test
    void cargarPromptRecordatorioConDatosVerificados() {

        String resultado = biblioteca.cargar(
                "recordatorio",
                Map.of(
                        "salon", "Barbería La Navaja",
                        "cliente", "Carlos Pérez",
                        "datos",
                        "Servicio: Corte clásico, hora: 10:30"
                )
        );

        assertNotNull(resultado);

        assertTrue(
                resultado.contains("Barbería La Navaja")
        );

        assertTrue(
                resultado.contains("Carlos Pérez")
        );

        assertTrue(
                resultado.contains("Servicio: Corte clásico")
        );

        assertFalse(resultado.contains("{{salon}}"));
        assertFalse(resultado.contains("{{cliente}}"));
        assertFalse(resultado.contains("{{datos}}"));
    }

    // PRUEBA 3
    // Debe impedir nombres de archivos
    // que intenten salir de la carpeta prompts.

    @Test
    void rechazarNombreDePromptInvalido() {

        assertThrows(
                IllegalArgumentException.class,
                () -> biblioteca.cargar(
                        "../../archivo",
                        Map.of()
                )
        );
    }

    // PRUEBA 4
    // Un prompt inexistente debe producir
    // un error controlado.

    @Test
    void rechazarPromptInexistente() {

        assertThrows(
                IllegalStateException.class,
                () -> biblioteca.cargar(
                        "prompt_inexistente",
                        Map.of()
                )
        );
    }
}
