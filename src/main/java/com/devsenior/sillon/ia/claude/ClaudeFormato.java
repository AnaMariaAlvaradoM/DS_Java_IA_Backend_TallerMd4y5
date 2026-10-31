package com.devsenior.sillon.ia.claude;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

final class ClaudeFormato {

    private ClaudeFormato() {
    }

    record Peticion(
            String model,
            @JsonProperty("max_tokens") int maxTokens,
            String system,
            List<Mensaje> messages,
            double temperature,
            Pensamiento thinking
    ) {
    }

    record Mensaje(String role, String content) {
    }

    record Pensamiento(String type) {
    }

    record Respuesta(
            List<Bloque> content,
            @JsonProperty("stop_reason") String stopReason,
            Uso usage
    ) {
    }

    record Bloque(String type, String text) {
    }

    record Uso(
            @JsonProperty("input_tokens") int inputTokens,
            @JsonProperty("cache_read_input_tokens") int cacheReadInputTokens,
            @JsonProperty("output_tokens") int outputTokens
    ) {
    }
}
