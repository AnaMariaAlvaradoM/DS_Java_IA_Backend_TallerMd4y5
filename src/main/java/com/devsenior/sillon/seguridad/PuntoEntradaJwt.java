package com.devsenior.sillon.seguridad;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 401: la ruta pide autenticacion y no llego un token valido. Responde con el formato unico de error. */
@Component
public class PuntoEntradaJwt implements AuthenticationEntryPoint {

    private final EscritorErrorHttp escritor;

    public PuntoEntradaJwt(EscritorErrorHttp escritor) {
        this.escritor = escritor;
    }

    @Override
    public void commence(HttpServletRequest peticion, HttpServletResponse respuesta,
            AuthenticationException excepcion) throws IOException, ServletException {
        escritor.escribir(respuesta, HttpStatus.UNAUTHORIZED,
                "Se requiere un token válido. Inicia sesión y vuelve a intentarlo.");
    }
}
