package com.devsenior.sillon.seguridad;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.devsenior.sillon.compartido.excepcion.ExcepcionNoAutenticado;
import com.devsenior.sillon.compartido.excepcion.ExcepcionNoPermitida;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ManejadorSeguridad
        implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final HandlerExceptionResolver resolvedor;

    public ManejadorSeguridad(
            @Qualifier("handlerExceptionResolver")
            HandlerExceptionResolver resolvedor) {

        this.resolvedor = resolvedor;
    }

    @Override
    public void commence(
            HttpServletRequest solicitud,
            HttpServletResponse respuesta,
            AuthenticationException excepcion)
            throws IOException, ServletException {

        resolvedor.resolveException(
                solicitud,
                respuesta,
                null,
                new ExcepcionNoAutenticado(
                        "Se requiere autenticación válida."));
    }

    @Override
    public void handle(
            HttpServletRequest solicitud,
            HttpServletResponse respuesta,
            AccessDeniedException excepcion)
            throws IOException, ServletException {

        resolvedor.resolveException(
                solicitud,
                respuesta,
                null,
                new ExcepcionNoPermitida(
                        "No tiene permisos para realizar esta operación."));
    }

}