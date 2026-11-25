package com.devsenior.sillon.ia;

import org.springframework.stereotype.Component;

import com.devsenior.sillon.compartido.PasarelaException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class LectorJson {

    private final ObjectMapper objectMapper;

    public LectorJson() {
        this.objectMapper = new ObjectMapper();
    }

    public JsonNode leerJson(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new PasarelaException("La respuesta de la IA está vacía.");
        }

        String jsonLimpio = limpiarMarkdown(texto);

        try {
            return objectMapper.readTree(jsonLimpio);
        } catch (Exception e) {
            throw new PasarelaException("La respuesta de la IA no es un JSON válido: " + e.getMessage());
        }
    }

    public JsonNode leerJson(String texto, String... clavesRequeridas) {
        JsonNode nodo = leerJson(texto);

        for (String clave : clavesRequeridas) {
            if (!nodo.has(clave) || nodo.get(clave).isNull()) {
                throw new PasarelaException("La respuesta de la IA no contiene la clave requerida: '" + clave + "'.");
            }
            if (nodo.get(clave).isTextual() && nodo.get(clave).asString().isBlank()) {
                throw new PasarelaException("La clave '" + clave + "' en la respuesta de la IA está vacía.");
            }
        }

        return nodo;
    }

    private String limpiarMarkdown(String texto) {
        String limpio = texto.trim();
        if (limpio.startsWith("```json")) {
            limpio = limpio.substring(7);
        } else if (limpio.startsWith("```")) {
            limpio = limpio.substring(3);
        }

        if (limpio.endsWith("```")) {
            limpio = limpio.substring(0, limpio.length() - 3);
        }

        return limpio.trim();
    }
}
