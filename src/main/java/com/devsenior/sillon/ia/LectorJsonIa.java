package com.devsenior.sillon.ia;

import com.devsenior.sillon.compartido.PasarelaException;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class LectorJsonIa {
    private final ObjectMapper json;

    public LectorJsonIa(ObjectMapper json) { this.json = json; }

    public JsonNode leer(String contenido, Map<String, Class<?>> campos) {
        try {
            JsonNode raiz = json.readTree(contenido);
            if (raiz == null || !raiz.isObject()) throw new PasarelaException("La IA no devolvió un objeto JSON");
            for (var campo : campos.entrySet()) {
                JsonNode valor = raiz.path(campo.getKey());
                if (valor.isMissingNode() || valor.isNull()
                        || (campo.getValue() == String.class && (!valor.isTextual() || valor.asText().isBlank()))
                        || (campo.getValue() == Boolean.class && !valor.isBoolean()))
                    throw new PasarelaException("Respuesta de IA incompleta o inválida: " + campo.getKey());
            }
            return raiz;
        } catch (PasarelaException error) {
            throw error;
        } catch (RuntimeException error) {
            throw new PasarelaException("La IA no devolvió JSON válido");
        }
    }
}
