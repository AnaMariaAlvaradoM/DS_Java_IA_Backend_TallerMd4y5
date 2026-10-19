package com.devsenior.sillon.ia;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class LectorJsonIA {

    private final ObjectMapper objectMapper;

    public LectorJsonIA(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T leer(RespuestaIA respuesta, Class<T> tipo) {
        if (respuesta == null || respuesta.texto() == null || respuesta.texto().isBlank()) {
            throw new RespuestaIAInvalidaException(null, null);
        }
        String texto = respuesta.texto();
        int inicio = texto.indexOf('{');
        int fin = texto.lastIndexOf('}');
        if (inicio < 0 || fin < inicio) {
            throw new RespuestaIAInvalidaException(respuesta.proveedor(), texto);
        }
        try {
            return objectMapper.readValue(texto.substring(inicio, fin + 1), tipo);
        } catch (JacksonException ex) {
            throw new RespuestaIAInvalidaException(respuesta.proveedor(), texto);
        }
    }
}
