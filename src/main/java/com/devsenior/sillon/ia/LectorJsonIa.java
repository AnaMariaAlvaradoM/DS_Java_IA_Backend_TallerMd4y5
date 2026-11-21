package com.devsenior.sillon.ia;

import com.devsenior.sillon.compartido.PasarelaException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class LectorJsonIa {
    private final ObjectMapper json;

    public LectorJsonIa(ObjectMapper json) {
        this.json = json;
    }

    public JsonNode leerObjeto(String texto) {
        try {
            JsonNode nodo = json.readTree(texto);
            if (nodo == null || !nodo.isObject()) {
                throw new PasarelaException("La IA no respondió un objeto JSON válido");
            }
            return nodo;
        } catch (RuntimeException ex) {
            throw new PasarelaException("La IA no respondió un objeto JSON válido");
        }
    }

    public String textoObligatorio(JsonNode nodo, String clave, int maximo) {
        JsonNode valor = nodo.path(clave);
        if (!valor.isTextual() || valor.asText().isBlank() || valor.asText().length() > maximo) {
            throw new PasarelaException("La IA respondió un valor inválido para " + clave);
        }
        return valor.asText();
    }

    public boolean booleanoObligatorio(JsonNode nodo, String clave) {
        JsonNode valor = nodo.path(clave);
        if (!valor.isBoolean()) {
            throw new PasarelaException("La IA respondió un valor inválido para " + clave);
        }
        return valor.asBoolean();
    }
}
