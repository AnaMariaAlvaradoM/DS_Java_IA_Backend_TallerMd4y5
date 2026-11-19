package com.devsenior.sillon.ia.service.claude;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Records del JSON de la API de mensajes con formato Claude. */
public final class ClaudeFormato {

    private ClaudeFormato() {
    }

    public record Peticion(
            String model,
            @JsonProperty("max_tokens") int maxTokens,
            String system,
            List<Mensaje> messages,
            double temperature
    ) {
    }

    public record Mensaje(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Respuesta(List<Bloque> content, Uso usage) {
    }

    /** Puede haber bloques de razonamiento o herramientas: se usa el de tipo "text", no el primero. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Bloque(String type, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Uso(
            @JsonProperty("input_tokens") Integer inputTokens,
            @JsonProperty("output_tokens") Integer outputTokens,
            @JsonProperty("cache_read_input_tokens") Integer cacheReadInputTokens
    ) {
    }
}
