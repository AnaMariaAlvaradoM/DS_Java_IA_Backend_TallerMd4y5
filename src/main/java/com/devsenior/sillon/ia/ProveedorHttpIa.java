package com.devsenior.sillon.ia;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class ProveedorHttpIa implements ProveedorIa {

    private final String nombre;
    private final boolean claude;
    private final ConfiguracionIa.Proveedor configuracion;
    private final RestClient cliente;
    private final ObjectMapper mapper;

    public ProveedorHttpIa(String nombre, boolean claude, ConfiguracionIa.Proveedor configuracion,
            RestClient cliente, ObjectMapper mapper) {
        this.nombre = nombre;
        this.claude = claude;
        this.configuracion = configuracion;
        this.cliente = cliente;
        this.mapper = mapper;
    }

    @Override
    public String nombre() {
        return nombre;
    }

    @Override
    public RespuestaIa responder(String instrucciones) {
        return responder(instrucciones, "Redacte el borrador solicitado.");
    }

    @Override
    public RespuestaIa responder(String instrucciones, String mensaje) {
        if (configuracion.llave() == null || configuracion.llave().isBlank()) {
            throw new ProveedorNoDisponibleException(true);
        }
        Map<String, Object> cuerpo = claude
                ? Map.of("model", configuracion.modelo(), "max_tokens", 1024, "system", instrucciones,
                        "messages", List.of(Map.of("role", "user", "content", mensaje)))
                : Map.of("model", configuracion.modelo(),
                        "messages", List.of(Map.of("role", "system", "content", instrucciones),
                                Map.of("role", "user", "content", mensaje)),
                        "response_format", Map.of("type", "json_object"));
        long inicio = System.nanoTime();
        String respuesta;
        try {
            respuesta = cliente.post().uri(claude ? "/messages" : "/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (claude) {
                            headers.set("x-api-key", configuracion.llave());
                            headers.set("anthropic-version", "2023-06-01");
                        } else {
                            headers.setBearerAuth(configuracion.llave());
                        }
                    }).body(cuerpo).retrieve().body(String.class);
        } catch (RestClientResponseException ex) {
            int estado = ex.getStatusCode().value();
            throw new ProveedorNoDisponibleException(estado >= 400 && estado < 500 && estado != 408 && estado != 429);
        } catch (ResourceAccessException ex) {
            throw new ProveedorNoDisponibleException(false);
        }
        long milisegundos = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
        if (respuesta == null || respuesta.isBlank()) {
            throw new RespuestaIaInvalidaException();
        }
        try {
            JsonNode json = mapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(respuesta);
            if (json == null || !json.isObject()) {
                throw new RespuestaIaInvalidaException();
            }
            JsonNode texto = claude ? json.path("content").path(0).path("text")
                    : json.path("choices").path(0).path("message").path("content");
            JsonNode entrada = json.path("usage").path(claude ? "input_tokens" : "prompt_tokens");
            JsonNode salida = json.path("usage").path(claude ? "output_tokens" : "completion_tokens");
            if (!texto.isString() || !tokenValido(entrada) || !tokenValido(salida)) {
                throw new RespuestaIaInvalidaException();
            }
            BigDecimal costo = configuracion.costoEntradaMillon().multiply(BigDecimal.valueOf(entrada.asLong()))
                    .add(configuracion.costoSalidaMillon().multiply(BigDecimal.valueOf(salida.asLong())))
                    .divide(BigDecimal.valueOf(1_000_000));
            return new RespuestaIa(texto.asString(), new FacturaIa(nombre, configuracion.modelo(),
                    entrada.asLong(), salida.asLong(), costo, milisegundos));
        } catch (JacksonException ex) {
            throw new RespuestaIaInvalidaException();
        }
    }

    private static boolean tokenValido(JsonNode nodo) {
        return nodo.isIntegralNumber() && nodo.canConvertToLong() && nodo.asLong() >= 0;
    }
}
