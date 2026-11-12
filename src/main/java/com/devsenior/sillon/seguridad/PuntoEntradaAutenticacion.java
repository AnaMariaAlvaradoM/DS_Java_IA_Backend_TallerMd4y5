package com.devsenior.sillon.seguridad;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;

@Component
public class PuntoEntradaAutenticacion implements AuthenticationEntryPoint {

    private final Clock reloj;

    public PuntoEntradaAutenticacion(Clock reloj) {
        this.reloj = reloj;
    }

    @Override
    public void commence(jakarta.servlet.http.HttpServletRequest solicitud,
            HttpServletResponse respuesta, AuthenticationException causa) throws IOException {
        responder(respuesta, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "Autenticación requerida");
    }

    public void responder(HttpServletResponse respuesta, int codigo, String error, String mensaje)
            throws IOException {
        respuesta.setStatus(codigo);
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.getWriter().write(construirJson(codigo, error, mensaje));
    }

    private String construirJson(int codigo, String error, String mensaje) {
        String fecha = LocalDateTime.now(reloj).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return "{\"codigo\":" + codigo
                + ",\"error\":\"" + escapar(error)
                + "\",\"mensaje\":\"" + escapar(mensaje)
                + "\",\"fecha\":\"" + fecha + "\"}";
    }

    private String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
