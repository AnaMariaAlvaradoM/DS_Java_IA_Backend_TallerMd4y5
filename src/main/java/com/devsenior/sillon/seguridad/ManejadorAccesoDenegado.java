
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
import tools.jackson.databind.json.JsonMapper;

@Component
public class ManejadorAccesoDenegado implements AccessDeniedHandler {

    private final Clock reloj;
    private final JsonMapper jsonMapper;

    public ManejadorAccesoDenegado(
            Clock reloj,
            JsonMapper jsonMapper) {

        this.reloj = reloj;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception) throws IOException {

        HttpStatus estado = HttpStatus.FORBIDDEN;

        RespuestaError error = new RespuestaError(
                estado.value(),
                estado.getReasonPhrase(),
                "No tienes permisos para realizar esta operación",
                LocalDateTime.now(reloj)
        );

        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        jsonMapper.writeValue(response.getOutputStream(), error);
    }
}
