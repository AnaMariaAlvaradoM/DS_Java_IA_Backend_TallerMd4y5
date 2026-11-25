package com.devsenior.sillon.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.devsenior.sillon.compartido.PasarelaException;

import tools.jackson.databind.JsonNode;

class LectorJsonTest {

    private final LectorJson lector = new LectorJson();

    @Test
    @DisplayName("Parsea correctamente un JSON simple")
    void parseaJsonSimple() {
        String json = "{\"mensaje\": \"Hola mundo\", \"escalar\": false}";
        JsonNode nodo = lector.leerJson(json, "mensaje", "escalar");

        assertNotNull(nodo);
        assertEquals("Hola mundo", nodo.get("mensaje").asString());
        assertEquals(false, nodo.get("escalar").asBoolean());
    }

    @Test
    @DisplayName("Limpia bloques markdown ```json ... ``` y parsea el contenido")
    void limpiaMarkdownJson() {
        String json = "```json\n{\"respuesta\": \"Atención personalizada\", \"escalar\": true}\n```";
        JsonNode nodo = lector.leerJson(json, "respuesta", "escalar");

        assertEquals("Atención personalizada", nodo.get("respuesta").asString());
        assertTrue(nodo.get("escalar").asBoolean());
    }

    @Test
    @DisplayName("Lanza PasarelaException cuando falta una clave requerida")
    void claveFaltante_lanzaPasarelaException() {
        String json = "{\"mensaje\": \"Hola\"}";
        assertThrows(PasarelaException.class,
                () -> lector.leerJson(json, "mensaje", "claveInexistente"));
    }

    @Test
    @DisplayName("Lanza PasarelaException cuando una clave requerida viene vacía")
    void claveVacia_lanzaPasarelaException() {
        String json = "{\"mensaje\": \"   \"}";
        assertThrows(PasarelaException.class,
                () -> lector.leerJson(json, "mensaje"));
    }

    @Test
    @DisplayName("Lanza PasarelaException cuando el texto no es JSON válido")
    void textoInvalido_lanzaPasarelaException() {
        assertThrows(PasarelaException.class,
                () -> lector.leerJson("No soy un JSON"));
    }
}
