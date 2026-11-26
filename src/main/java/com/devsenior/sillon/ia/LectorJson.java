package com.devsenior.sillon.ia;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.devsenior.sillon.compartido.PasarelaException;

import tools.jackson.databind.json.JsonMapper;

/** Lee el JSON que devolvió la IA. Si no es JSON válido o le faltan datos: 502. */
@Component
public class LectorJson {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @SuppressWarnings("unchecked")
    public Map<String, Object> leer(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new PasarelaException("La IA respondió vacío");
        }
        String limpio = texto.strip();
        // Algunos modelos envuelven el JSON en ```json ... ```
        if (limpio.startsWith("```")) {
            limpio = limpio.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        }
        try {
            return mapper.readValue(limpio, Map.class);
        } catch (RuntimeException e) {
            throw new PasarelaException("La IA respondió un valor que no es JSON válido");
        }
    }

    public String textoObligatorio(Map<String, Object> json, String clave, int maximo) {
        Object valor = json.get(clave);
        if (!(valor instanceof String s) || s.isBlank()) {
            throw new PasarelaException("La IA no devolvió el campo " + clave);
        }
        if (s.length() > maximo) {
            throw new PasarelaException("El campo " + clave + " de la IA supera " + maximo + " caracteres");
        }
        return s.strip();
    }

    public boolean booleanoObligatorio(Map<String, Object> json, String clave) {
        Object valor = json.get(clave);
        if (!(valor instanceof Boolean b)) {
            throw new PasarelaException("La IA no devolvió el campo " + clave + " como booleano");
        }
        return b;
    }
}
