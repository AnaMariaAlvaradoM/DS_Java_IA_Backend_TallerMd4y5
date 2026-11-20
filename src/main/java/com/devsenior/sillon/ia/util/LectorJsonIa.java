package com.devsenior.sillon.ia.util;

import com.devsenior.sillon.compartido.excepcion.ErrorPasarelaException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class LectorJsonIa {

    private final ObjectMapper objectMapper;

    public LectorJsonIa(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode leerJson(String textoBruto) {
        try {
            String limpio = limpiarBloquesMarkdown(textoBruto);
            return objectMapper.readTree(limpio);
        } catch (Exception e) {
            throw new ErrorPasarelaException("La IA no devolvió un JSON válido");
        }
    }

    private String limpiarBloquesMarkdown(String texto) {
        if (texto == null) {
            return "";
        }
        String t = texto.trim();
        if (t.startsWith("```")) {
            int primerSalto = t.indexOf('\n');
            if (primerSalto != -1) {
                t = t.substring(primerSalto + 1);
            }
            if (t.endsWith("```")) {
                t = t.substring(0, t.length() - 3);
            }
        }
        return t.trim();
    }
}