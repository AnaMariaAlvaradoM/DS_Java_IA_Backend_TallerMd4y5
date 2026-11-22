package com.devsenior.sillon.ia.claude;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

final class ClaudeFormato {

    private ClaudeFormato() {
    }

    record Peticion(
            String model,
            @JsonProperty("max_tokens") int maxTokens,
            String system,
            List<Mensaje> messages,
            double temperature) {
    }

    record Mensaje(String role, String content) {
    }

    record Respuesta(
            List<Bloque> content,
            @JsonProperty("stop_reason") String stopReason,
            Uso usage) {
    }

    record Bloque(String type, String text) {
    }

    record Uso(
            @JsonProperty("input_tokens") int inputTokens,
            @JsonProperty("cache_read_input_tokens") int cacheReadInputTokens,
            @JsonProperty("output_tokens") int outputTokens) {
    }
}
