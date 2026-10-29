package com.devsenior.sillon.seguridad;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.devsenior.sillon.compartido.ErrorRespuesta;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public JwtAccessDeniedHandler(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        HttpStatus estado = HttpStatus.FORBIDDEN;
        ErrorRespuesta error = new ErrorRespuesta(estado.value(), estado.getReasonPhrase(),
                "No tiene permisos para esta accion", LocalDateTime.now(clock));
        response.setStatus(estado.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), error);
    }
}
