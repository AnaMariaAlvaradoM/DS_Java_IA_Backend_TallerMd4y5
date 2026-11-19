package com.devsenior.sillon.ia.service;

import com.devsenior.sillon.ia.excepcion.RespuestaIAInvalidaException;
import com.devsenior.sillon.ia.model.RespuestaIA;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * La salida de la IA es entrada externa: se valida antes de usarla.
 * Toma el texto entre la primera "{" y la última "}" (tolera ```json y texto alrededor) y lo convierte con Jackson 3.
 */
@Component
public class LectorJsonIA {

    private final ObjectMapper objectMapper;

    public LectorJsonIA(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T leer(RespuestaIA respuesta, Class<T> tipo) {
        String texto = respuesta.texto() == null ? "" : respuesta.texto();
        int inicio = texto.indexOf('{');
        int fin = texto.lastIndexOf('}');
        if (inicio < 0 || fin <= inicio) {
            throw new RespuestaIAInvalidaException("no contiene un objeto JSON");
        }
        try {
            T valor = objectMapper.readValue(texto.substring(inicio, fin + 1), tipo);
            if (valor == null) {
                throw new RespuestaIAInvalidaException("el JSON está vacío");
            }
            return valor;
        } catch (JacksonException ex) {
            throw new RespuestaIAInvalidaException("el JSON no tiene el formato esperado");
        }
    }
}
