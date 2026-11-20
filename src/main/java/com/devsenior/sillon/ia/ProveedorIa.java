package com.devsenior.sillon.ia;

import static com.devsenior.sillon.compartido.Fallos.fallo;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.*;

import java.io.IOException;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ProveedorIa {
    public record Factura(
            String proveedor,
            String modelo,
            int tokensEntrada,
            int tokensSalida,
            BigDecimal costoEstimadoUsd,
            long milisegundos) {}

    public record Resultado(String texto, Factura factura) {}

    private static final Logger log = LoggerFactory.getLogger(ProveedorIa.class);
    private final RestClient http;
    private final ObjectMapper json;
    private final String url, clave, modelo, urlClaude, claveClaude, modeloClaude;
    private final BigDecimal entrada, salida, entradaClaude, salidaClaude;
    private final String promptChat, promptRecordatorio;

    public String promptChat() {
        return promptChat;
    }

    public String promptRecordatorio() {
        return promptRecordatorio;
    }

    ProveedorIa(
            RestClient http,
            ObjectMapper json,
            @Value("${sillon.ia.openai.url}") String url,
            @Value("${sillon.ia.clave}") String clave,
            @Value("${sillon.ia.modelo}") String modelo,
            @Value("${sillon.ia.claude.url}") String urlClaude,
            @Value("${sillon.ia.claude.clave}") String claveClaude,
            @Value("${sillon.ia.claude.modelo}") String modeloClaude,
            @Value("${sillon.ia.entrada}") BigDecimal entrada,
            @Value("${sillon.ia.salida}") BigDecimal salida,
            @Value("${sillon.ia.claude.entrada}") BigDecimal entradaClaude,
            @Value("${sillon.ia.claude.salida}") BigDecimal salidaClaude)
            throws IOException {
        this.http = http;
        this.json = json;
        this.url = url.replaceFirst("/+$", "");
        this.clave = clave;
        this.modelo = modelo;
        this.urlClaude = urlClaude.replaceFirst("/+$", "");
        this.claveClaude = claveClaude;
        this.modeloClaude = modeloClaude;
        this.entrada = entrada;
        this.salida = salida;
        this.entradaClaude = entradaClaude;
        this.salidaClaude = salidaClaude;
        this.promptChat = plantilla("chatbot");
        this.promptRecordatorio = plantilla("recordatorio");
    }

    private String plantilla(String nombre) throws IOException {
        return new ClassPathResource("prompts/" + nombre + ".txt")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    public Resultado consultar(String sistema, String conversacion) {
        if (!clave.isBlank()) {
            try {
                return gemini(sistema, conversacion);
            } catch (RestClientException e) {
                log.warn("Gemini no está disponible ({})", e.getClass().getSimpleName());
            }
        } else log.warn("Gemini carece de clave configurada");
        if (!claveClaude.isBlank()) {
            try {
                return claude(sistema, conversacion);
            } catch (RestClientException e) {
                log.warn("Claude no está disponible ({})", e.getClass().getSimpleName());
            }
        } else log.warn("Claude carece de clave configurada");
        throw fallo(503, "Ningún proveedor de IA está disponible");
    }

    private Resultado gemini(String sistema, String conversacion) {
        long inicio = System.nanoTime();
        var r =
                http.post()
                        .uri(url + "/chat/completions")
                        .headers(h -> h.setBearerAuth(clave))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "model",
                                        modelo,
                                        "messages",
                                        List.of(
                                                Map.of("role", "system", "content", sistema),
                                                Map.of("role", "user", "content", conversacion)),
                                        "response_format",
                                        Map.of("type", "json_object")))
                        .retrieve()
                        .body(JsonNode.class);
        if (r == null) throw fallo(502, "Respuesta vacía de Gemini");
        var u = r.path("usage");
        return new Resultado(
                r.path("choices").path(0).path("message").path("content").asText(""),
                factura(
                        "Gemini",
                        modelo,
                        u.path("prompt_tokens").asInt(),
                        u.path("completion_tokens").asInt(),
                        inicio,
                        entrada,
                        salida));
    }

    private Resultado claude(String sistema, String conversacion) {
        long inicio = System.nanoTime();
        var r =
                http.post()
                        .uri(urlClaude + "/v1/messages")
                        .header("x-api-key", claveClaude)
                        .header("anthropic-version", "2023-06-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "model",
                                        modeloClaude,
                                        "max_tokens",
                                        512,
                                        "system",
                                        sistema,
                                        "messages",
                                        List.of(Map.of("role", "user", "content", conversacion))))
                        .retrieve()
                        .body(JsonNode.class);
        if (r == null) throw fallo(502, "Respuesta vacía de Claude");
        var u = r.path("usage");
        return new Resultado(
                r.path("content").path(0).path("text").asText(""),
                factura(
                        "Claude",
                        modeloClaude,
                        u.path("input_tokens").asInt(),
                        u.path("output_tokens").asInt(),
                        inicio,
                        entradaClaude,
                        salidaClaude));
    }

    private Factura factura(
            String nombre,
            String modelo,
            int in,
            int out,
            long inicio,
            BigDecimal precioIn,
            BigDecimal precioOut) {
        var costo =
                precioIn.multiply(BigDecimal.valueOf(in))
                        .add(precioOut.multiply(BigDecimal.valueOf(out)))
                        .movePointLeft(6);
        return new Factura(
                nombre, modelo, in, out, costo, (System.nanoTime() - inicio) / 1_000_000L);
    }

    public JsonNode leer(String texto) {
        try {
            var objeto = json.readTree(texto);
            if (objeto != null && objeto.isObject()) return objeto;
        } catch (JacksonException ignored) {
        }
        throw fallo(502, "La IA debe devolver un objeto JSON válido");
    }
}
