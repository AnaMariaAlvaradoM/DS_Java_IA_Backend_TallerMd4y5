package com.devsenior.sillon.ia;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Component
@Order(2)
public class ProveedorClaude implements ProveedorIa {
    private final RestClient cliente;
    private final String llave;
    private final String modelo;

    public ProveedorClaude(ClienteHttpIa fabrica,
                           @Value("${sillon.ia.claude.url:https://api.anthropic.com}") String url,
                           @Value("${sillon.ia.claude.llave:}") String llave,
                           @Value("${sillon.ia.claude.modelo:claude-sonnet-4-5}") String modelo) {
        this.cliente = fabrica.crear(url);
        this.llave = llave;
        this.modelo = modelo;
    }

    @Override
    public String nombre() { return "Claude"; }

    @Override
    public RespuestaIa generar(String instrucciones, String mensajeUsuario) {
        if (llave.isBlank() || llave.contains("placeholder")) {
            throw new IllegalStateException("No hay llave de Claude configurada");
        }
        long inicio = System.nanoTime();
        Map<String, Object> cuerpo = Map.of("model", modelo, "max_tokens", 700,
                "system", instrucciones,
                "messages", List.of(Map.of("role", "user", "content", mensajeUsuario)));
        JsonNode respuesta = cliente.post().uri("/v1/messages")
                .header("x-api-key", llave)
                .header("anthropic-version", "2023-06-01")
                .body(cuerpo).retrieve().body(JsonNode.class);
        if (respuesta == null || respuesta.path("content").isEmpty()) {
            throw new IllegalArgumentException("Claude respondió sin contenido");
        }
        String texto = respuesta.path("content").get(0).path("text").asText();
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Claude respondió sin texto");
        }
        JsonNode uso = respuesta.path("usage");
        return new RespuestaIa(texto, new FacturaIa(nombre(), respuesta.path("model").asText(modelo),
                uso.path("input_tokens").asInt(0), uso.path("output_tokens").asInt(0),
                BigDecimal.ZERO, (System.nanoTime() - inicio) / 1_000_000));
    }
}
