package com.devsenior.sillon.ia.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

final class OpenAiFormato {

    private OpenAiFormato() {
    }

    record Peticion(
            String model,
            List<Mensaje> messages,
            @JsonProperty("max_tokens") int maxTokens
    ) {
    }

    record Mensaje(String role, String content) {
    }

    record Respuesta(List<Opcion> choices, Uso usage) {
    }

    record Opcion(Mensaje message, @JsonProperty("finish_reason") String finishReason) {
    }

    record Uso(
            @JsonProperty("prompt_tokens") int promptTokens,
            @JsonProperty("completion_tokens") int completionTokens
    ) {
    }
}
