package com.devsenior.sillon.seguridad;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 403: el usuario esta autenticado, pero su rol no puede usar esta ruta. Responde con el formato unico de error. */
@Component
public class AccesoDenegadoJwt implements AccessDeniedHandler {

    private final EscritorErrorHttp escritor;

    public AccesoDenegadoJwt(EscritorErrorHttp escritor) {
        this.escritor = escritor;
    }

    @Override
    public void handle(HttpServletRequest peticion, HttpServletResponse respuesta,
            AccessDeniedException excepcion) throws IOException, ServletException {
        escritor.escribir(respuesta, HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta acción.");
    }
}
