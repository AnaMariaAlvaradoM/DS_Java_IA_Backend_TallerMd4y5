package com.devsenior.sillon.seguridad;

import com.devsenior.sillon.compartido.dto.ErrorRespuesta;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final Clock clock;
    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint(Clock clock) {
        this.clock = clock;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorRespuesta error = new ErrorRespuesta(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "No autenticado o token inválido",
                LocalDateTime.now(clock)
        );

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
