
package com.devsenior.sillon.ia;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class LectorJsonIA {

    private final JsonMapper jsonMapper;

    public LectorJsonIA(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public JsonNode leerObjeto(String contenido) {

        if (contenido == null || contenido.isBlank()) {
            throw new IllegalStateException(
                    "La IA devolvió una respuesta vacía"
            );
        }

        String texto = limpiarFormato(contenido);

        JsonNode objeto;

        try {
            objeto = jsonMapper.readTree(texto);
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                    "La IA devolvió un JSON inválido",
                    ex
            );
        }

        if (objeto == null || !objeto.isObject()) {
            throw new IllegalStateException(
                    "La respuesta de IA debe ser un objeto JSON"
            );
        }

        return objeto;
    }

    public String leerTextoObligatorio(
            JsonNode objeto,
            String campo
    ) {
        JsonNode valor = objeto.get(campo);

        if (valor == null
                || !valor.isTextual()
                || valor.asText().isBlank()) {
            throw new IllegalStateException(
                    "Falta el campo de texto: " + campo
            );
        }

        return valor.asText().trim();
    }

    public boolean leerBooleanoObligatorio(
            JsonNode objeto,
            String campo
    ) {
        JsonNode valor = objeto.get(campo);

        if (valor == null || !valor.isBoolean()) {
            throw new IllegalStateException(
                    "Falta el campo booleano: " + campo
            );
        }

        return valor.asBoolean();
    }

    private String limpiarFormato(String contenido) {

        String texto = contenido.trim();

        if (texto.startsWith("```")) {
            int salto = texto.indexOf('\n');

            if (salto >= 0 && texto.endsWith("```")) {
                texto = texto.substring(
                        salto + 1,
                        texto.length() - 3
                ).trim();
            }
        }

        return texto;
    }
}
