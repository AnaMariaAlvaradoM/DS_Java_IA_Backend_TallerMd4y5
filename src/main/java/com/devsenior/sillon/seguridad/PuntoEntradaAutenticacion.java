
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
import tools.jackson.databind.json.JsonMapper;

@Component
public class PuntoEntradaAutenticacion implements AuthenticationEntryPoint {

    private final Clock reloj;
    private final JsonMapper jsonMapper;

    public PuntoEntradaAutenticacion(
            Clock reloj,
            JsonMapper jsonMapper) {

        this.reloj = reloj;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {

        HttpStatus estado = HttpStatus.UNAUTHORIZED;

        RespuestaError error = new RespuestaError(
                estado.value(),
                estado.getReasonPhrase(),
                "No autorizado. Debes iniciar sesión o proporcionar un token válido",
                LocalDateTime.now(reloj)
        );

        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        jsonMapper.writeValue(response.getOutputStream(), error);
    }
}
