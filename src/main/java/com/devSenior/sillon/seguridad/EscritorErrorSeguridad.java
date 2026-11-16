package com.devSenior.sillon.seguridad;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.http.MediaType;

import com.devSenior.sillon.compartido.dto.ErrorRespuesta;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

final class EscritorErrorSeguridad {

	private EscritorErrorSeguridad() {
	}

	static void escribir(HttpServletResponse response, ObjectMapper mapper, Clock reloj, int codigo, String error,
			String mensaje) throws IOException {
		response.setStatus(codigo);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		ErrorRespuesta cuerpo = new ErrorRespuesta(codigo, error, mensaje,
				LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS));
		mapper.writeValue(response.getWriter(), cuerpo);
	}

}
