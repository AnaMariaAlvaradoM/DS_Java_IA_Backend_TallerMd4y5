package com.devsenior.sillon.ia.service.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Records del JSON de Chat Completions (formato OpenAI, que Gemini también habla). */
public final class OpenAiFormato {

    private OpenAiFormato() {
    }

    public record Peticion(
            String model,
            List<Mensaje> messages,
            @JsonProperty("max_tokens") int maxTokens,
            double temperature
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Mensaje(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Respuesta(List<Opcion> choices, Uso usage) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Opcion(Mensaje message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Uso(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens
    ) {
    }
}
