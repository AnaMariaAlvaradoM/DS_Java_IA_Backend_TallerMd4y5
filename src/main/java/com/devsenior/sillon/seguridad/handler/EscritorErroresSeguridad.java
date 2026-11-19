package com.devsenior.sillon.seguridad.handler;

import com.devsenior.sillon.compartido.excepcion.FabricaErrores;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Responde 401 (sin token o token inválido) y 403 (rol sin permiso) con el formato único de error.
 * Sin esto, Spring Security respondería con su propio formato o con un 403 en lugar de 401.
 */
@Component
public class EscritorErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final FabricaErrores fabricaErrores;
    private final ObjectMapper objectMapper;

    public EscritorErroresSeguridad(FabricaErrores fabricaErrores, ObjectMapper objectMapper) {
        this.fabricaErrores = fabricaErrores;
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        escribir(response, HttpStatus.UNAUTHORIZED, "Debe iniciar sesión con un token válido");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        escribir(response, HttpStatus.FORBIDDEN, "Su rol no tiene permiso para esta acción");
    }

    private void escribir(HttpServletResponse response, HttpStatus estado, String mensaje) throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(fabricaErrores.crear(estado, mensaje)));
    }
}
