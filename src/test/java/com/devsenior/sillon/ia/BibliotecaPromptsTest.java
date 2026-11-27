
package com.devsenior.sillon.ia;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BibliotecaPromptsTest {

    private BibliotecaPrompts biblioteca;

    @BeforeEach
    void preparar() {
        biblioteca = new BibliotecaPrompts();
    }

    @Test
    void cargaPlantillaExistente() {

        String contenido = biblioteca.cargar("prueba-biblioteca.txt");

        assertTrue(contenido.contains("Hola {{cliente}}"));
        assertTrue(contenido.contains("{{servicio}}"));
        assertTrue(contenido.contains("{{referencia}}"));
    }

    @Test
    void sustituyeMultiplesVariables() {

        String resultado = biblioteca.procesar(
                "prueba-biblioteca.txt",
                Map.of(
                        "cliente", "Ana",
                        "servicio", "Corte clasico",
                        "referencia", "CITA-123"));

        assertTrue(resultado.contains("Hola Ana"));
        assertTrue(resultado.contains("Corte clasico"));
        assertTrue(resultado.contains("CITA-123"));
        assertFalse(resultado.contains("{{cliente}}"));
    }

    @Test
    void conservaCaracteresEspeciales() {

        String resultado = biblioteca.procesar(
                "prueba-biblioteca.txt",
                Map.of(
                        "cliente", "Ana",
                        "servicio", "Corte $25",
                        "referencia", "ABC\\123"));

        assertTrue(resultado.contains("Corte $25"));
        assertTrue(resultado.contains("ABC\\123"));
    }

    @Test
    void rechazaPlantillaInexistente() {

        assertThrows(
                IllegalArgumentException.class,
                () -> biblioteca.cargar("inexistente.txt"));
    }

    @Test
    void rechazaVariableFaltante() {

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> biblioteca.procesar(
                        "prueba-biblioteca.txt",
                        Map.of(
                                "cliente", "Ana",
                                "servicio", "Corte")));

        assertTrue(
                excepcion.getMessage()
                        .contains("referencia"));
    }

    @Test
    void rechazaNombreArchivoInvalido() {

        assertThrows(
                IllegalArgumentException.class,
                () -> biblioteca.cargar("../secrets.properties"));
    }
}
