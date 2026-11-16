package com.devSenior.sillon.seguridad;

import java.io.IOException;
import java.time.Clock;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
public class ManejadorAccesoDenegado implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;
	private final Clock reloj;

	public ManejadorAccesoDenegado(ObjectMapper objectMapper, Clock reloj) {
		this.objectMapper = objectMapper;
		this.reloj = reloj;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
			throws IOException {
		EscritorErrorSeguridad.escribir(response, objectMapper, reloj, 403, "Forbidden",
				"No tiene permiso para esta acción");
	}

}
