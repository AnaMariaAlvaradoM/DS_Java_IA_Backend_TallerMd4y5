package com.devSenior.sillon.seguridad;

import java.io.IOException;
import java.time.Clock;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
public class EntradaAutenticacion implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;
	private final Clock reloj;

	public EntradaAutenticacion(ObjectMapper objectMapper, Clock reloj) {
		this.objectMapper = objectMapper;
		this.reloj = reloj;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
			throws IOException {
		EscritorErrorSeguridad.escribir(response, objectMapper, reloj, 401, "Unauthorized", "No autenticado");
	}

}
