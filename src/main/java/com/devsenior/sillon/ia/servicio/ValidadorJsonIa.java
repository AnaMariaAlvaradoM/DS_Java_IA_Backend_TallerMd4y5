
package com.devsenior.sillon.ia.servicio;

import org.springframework.stereotype.Component;

import com.devsenior.sillon.compartido.excepcion.ExcepcionIntegracion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ValidadorJsonIa {

    private final ObjectMapper objectMapper;

    public ValidadorJsonIa(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode interpretar(String contenido) {

        if (contenido == null || contenido.isBlank()) {
            throw new ExcepcionIntegracion(
                    "La IA devolvió una respuesta vacía");
        }

        try {
            JsonNode respuesta = objectMapper.readTree(contenido);

            if (respuesta == null || !respuesta.isObject()) {
                throw new ExcepcionIntegracion(
                        "La IA no devolvió un objeto JSON válido");
            }

            return respuesta;

        } catch (com.fasterxml.jackson.core.JsonProcessingException excepcion) {
            throw new ExcepcionIntegracion(
                    "La IA devolvió un JSON inválido");
        }
    }

    public String obtenerTexto(JsonNode respuesta, String campo) {

        JsonNode valor = respuesta.get(campo);

        if (valor == null
                || !valor.isTextual()
                || valor.asText().isBlank()) {
            throw new ExcepcionIntegracion(
                    "La respuesta de IA no contiene el campo "
                            + campo);
        }

        return valor.asText();
    }

    public boolean obtenerBooleano(JsonNode respuesta, String campo) {

        JsonNode valor = respuesta.get(campo);

        if (valor == null || !valor.isBoolean()) {
            throw new ExcepcionIntegracion(
                    "La respuesta de IA no contiene el campo "
                            + campo);
        }

        return valor.asBoolean();
    }
}
