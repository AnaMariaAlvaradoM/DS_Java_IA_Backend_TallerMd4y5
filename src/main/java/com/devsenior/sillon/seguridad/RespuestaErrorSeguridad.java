package com.devsenior.sillon.seguridad;

import com.devsenior.sillon.compartido.ErrorRespuesta;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class RespuestaErrorSeguridad {
    private final Clock clock;
    private final ObjectMapper json;

    public RespuestaErrorSeguridad(Clock clock, ObjectMapper json) {
        this.clock = clock;
        this.json = json;
    }

    public void escribir(HttpServletResponse respuesta, HttpStatus estado, String mensaje) throws IOException {
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(respuesta.getOutputStream(), new ErrorRespuesta(estado.value(), estado.getReasonPhrase(),
                mensaje, OffsetDateTime.now(clock)));
    }
}
