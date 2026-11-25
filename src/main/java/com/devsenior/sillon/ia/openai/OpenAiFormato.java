package com.devsenior.sillon.ia.openai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class OpenAiFormato {

    private OpenAiFormato() {
    }

    public record Peticion(
            String model,
            List<Mensaje> messages,
            @JsonProperty("max_tokens") int maxTokens,
            double temperature) {
    }

    public record Mensaje(String role, String content) {
    }

    public record Respuesta(List<Opcion> choices, Uso usage) {
    }

    public record Opcion(Mensaje message, @JsonProperty("finish_reason") String finishReason) {
    }

    public record Uso(
            @JsonProperty("prompt_tokens") int promptTokens,
            @JsonProperty("completion_tokens") int completionTokens) {
    }
}
