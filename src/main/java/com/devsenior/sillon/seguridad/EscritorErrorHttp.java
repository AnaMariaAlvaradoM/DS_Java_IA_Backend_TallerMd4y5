package com.devsenior.sillon.seguridad;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.devsenior.sillon.compartido.ErrorRespuesta;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Escribe el formato unico de error directamente en la respuesta HTTP.
 * Las excepciones de seguridad ocurren en los filtros, antes de llegar a los controladores,
 * asi que el @RestControllerAdvice no las ve: hay que escribirlas a mano.
 * (Jackson 3: el ObjectMapper esta en tools.jackson.databind.)
 */
@Component
public class EscritorErrorHttp {

    private final ObjectMapper objectMapper;
    private final Clock reloj;

    public EscritorErrorHttp(ObjectMapper objectMapper, Clock reloj) {
        this.objectMapper = objectMapper;
        this.reloj = reloj;
    }

    public void escribir(HttpServletResponse respuesta, HttpStatus estado, String mensaje) throws IOException {
        ErrorRespuesta cuerpo = ErrorRespuesta.de(estado, mensaje, reloj);
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        respuesta.getOutputStream().write(objectMapper.writeValueAsBytes(cuerpo));
    }
}
