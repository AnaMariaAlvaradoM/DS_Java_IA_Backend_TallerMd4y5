package com.devsenior.sillon.seguridad;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ManejadorAccesoDenegado implements AccessDeniedHandler {

    private final PuntoEntradaAutenticacion puntoDeEntrada;

    public ManejadorAccesoDenegado(PuntoEntradaAutenticacion puntoDeEntrada) {
        this.puntoDeEntrada = puntoDeEntrada;
    }

    @Override
    public void handle(HttpServletRequest solicitud, HttpServletResponse respuesta,
            AccessDeniedException causa) throws IOException {
        puntoDeEntrada.responder(respuesta, HttpServletResponse.SC_FORBIDDEN, "Forbidden",
                "No tienes permiso para este recurso");
    }
}
