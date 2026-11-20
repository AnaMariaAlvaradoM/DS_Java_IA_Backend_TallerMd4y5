package com.devsenior.sillon.seguridad;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.devsenior.sillon.compartido.RespuestaError;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/** 401 sin token o con token inválido, con el formato único de error (paso 2.4). */
@Component
public class PuntoEntradaAutenticacion implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PuntoEntradaAutenticacion(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        RespuestaError cuerpo = new RespuestaError(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Se requiere un token válido para acceder a este recurso",
                LocalDateTime.now(clock));
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(cuerpo));
    }
}
