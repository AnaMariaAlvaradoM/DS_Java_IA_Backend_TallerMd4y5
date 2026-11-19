package com.devsenior.sillon.ia;

import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class LectorJsonIa {

    private final ObjectMapper mapper;

    public LectorJsonIa(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String mensaje(String texto) {
        JsonNode json = objeto(texto);
        if (!json.path("mensaje").isString()) {
            throw new RespuestaIaInvalidaException();
        }
        String mensaje = json.path("mensaje").asString();
        if (mensaje.isBlank() || mensaje.length() > 700) {
            throw new RespuestaIaInvalidaException();
        }
        return mensaje;
    }

    public JsonNode objeto(String texto) {
        JsonNode json;
        try {
            json = mapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(texto);
        } catch (JacksonException ex) {
            throw new RespuestaIaInvalidaException();
        }
        if (json == null || !json.isObject()) {
            throw new RespuestaIaInvalidaException();
        }
        return json;
    }
}
