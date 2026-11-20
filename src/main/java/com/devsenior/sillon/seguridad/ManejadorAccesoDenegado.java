package com.devsenior.sillon.seguridad;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.devsenior.sillon.compartido.RespuestaError;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/** 403 cuando el rol no tiene permiso, con el formato único de error (paso 2.4). */
@Component
public class ManejadorAccesoDenegado implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ManejadorAccesoDenegado(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        RespuestaError cuerpo = new RespuestaError(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "Su rol no tiene permiso para esta acción",
                LocalDateTime.now(clock));
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(cuerpo));
    }
}
