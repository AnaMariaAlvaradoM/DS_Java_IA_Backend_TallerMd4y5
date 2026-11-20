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
@Order(1)
public class ProveedorOpenAi implements ProveedorIa {
    private final RestClient cliente;
    private final String llave;
    private final String modelo;

    public ProveedorOpenAi(ClienteHttpIa fabrica,
                           @Value("${sillon.ia.openai.url}") String url,
                           @Value("${sillon.ia.gemini.llave}") String llave,
                           @Value("${sillon.ia.openai.modelo:gemini-3.8-flash}") String modelo) {
        this.cliente = fabrica.crear(url);
        this.llave = llave;
        this.modelo = modelo;
    }

    @Override
    public String nombre() { return "Gemini"; }

    @Override
    public RespuestaIa generar(String instrucciones, String mensajeUsuario) {
        if (llave.isBlank() || "gemini_placeholder".equals(llave)) {
            throw new IllegalStateException("No hay llave de Gemini configurada");
        }
        long inicio = System.nanoTime();
        Map<String, Object> cuerpo = Map.of(
                "model", modelo,
                "messages", List.of(Map.of("role", "system", "content", instrucciones),
                        Map.of("role", "user", "content", mensajeUsuario)));
        JsonNode respuesta = cliente.post().uri("/chat/completions")
                .headers(headers -> headers.setBearerAuth(llave))
                .body(cuerpo).retrieve().body(JsonNode.class);
        if (respuesta == null || respuesta.path("choices").isEmpty()) {
            throw new IllegalArgumentException("Gemini respondió sin alternativas");
        }
        String texto = respuesta.path("choices").get(0).path("message").path("content").asText();
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Gemini respondió sin contenido");
        }
        JsonNode uso = respuesta.path("usage");
        FacturaIa factura = new FacturaIa(nombre(), respuesta.path("model").asText(modelo),
                uso.path("prompt_tokens").asInt(0), uso.path("completion_tokens").asInt(0),
                BigDecimal.ZERO, (System.nanoTime() - inicio) / 1_000_000);
        return new RespuestaIa(texto, factura);
    }
}
