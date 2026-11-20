package com.devsenior.sillon.seguridad;

import com.devsenior.sillon.compartido.dto.ErrorRespuesta;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final Clock clock;
    private final ObjectMapper objectMapper;

    public CustomAccessDeniedHandler(Clock clock) {
        this.clock = clock;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorRespuesta error = new ErrorRespuesta(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "No tiene permisos para realizar esta acción",
                LocalDateTime.now(clock)
        );

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
