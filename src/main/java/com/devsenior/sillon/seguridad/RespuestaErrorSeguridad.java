package com.devsenior.sillon.seguridad;

import com.devsenior.sillon.compartido.FabricaErrorApi;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class RespuestaErrorSeguridad {
    private final FabricaErrorApi fabrica;
    private final ObjectMapper mapper;

    public RespuestaErrorSeguridad(FabricaErrorApi fabrica, ObjectMapper mapper) {
        this.fabrica = fabrica;
        this.mapper = mapper;
    }

    public void escribir(HttpServletResponse response, HttpStatus estado, String mensaje) throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), fabrica.crear(estado, mensaje));
    }
}
