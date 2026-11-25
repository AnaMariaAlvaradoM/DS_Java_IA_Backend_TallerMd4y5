package com.devsenior.sillon.ia.claude;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class ClaudeFormato {

    private ClaudeFormato() {
    }

    public record Peticion(
            String model,
            @JsonProperty("max_tokens") int maxTokens,
            String system,
            List<Mensaje> messages,
            double temperature) {
    }

    public record Mensaje(String role, String content) {
    }

    public record Respuesta(
            List<Bloque> content,
            @JsonProperty("stop_reason") String stopReason,
            Uso usage) {
    }

    public record Bloque(String type, String text) {
    }

    public record Uso(
            @JsonProperty("input_tokens") int inputTokens,
            @JsonProperty("output_tokens") int outputTokens) {
    }
}
