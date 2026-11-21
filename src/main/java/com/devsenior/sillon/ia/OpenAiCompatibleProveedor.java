package com.devsenior.sillon.ia;

import com.devsenior.sillon.ia.dto.FacturaIaDTO;
import com.devsenior.sillon.ia.dto.RespuestaIa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Component
@Order(1)
public class OpenAiCompatibleProveedor implements ProveedorIa {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String openaiUrl;
    private final String apiKey;
    private final String modeloPorDefecto;

    @Autowired
    public OpenAiCompatibleProveedor(
            ObjectMapper objectMapper,
            @Value("${sillon.ia.openai.url:https://generativelanguage.googleapis.com/v1beta/openai}") String openaiUrl,
            @Value("${sillon.ia.gemini.llave:relleno_gemini_key}") String apiKey,
            @Value("${sillon.ia.modelo:gemini-2.5-flash}") String modeloPorDefecto
    ) {
        this(RestClient.create(), objectMapper, openaiUrl, apiKey, modeloPorDefecto);
    }

    public OpenAiCompatibleProveedor(
            RestClient restClient,
            ObjectMapper objectMapper,
            String openaiUrl,
            String apiKey,
            String modeloPorDefecto
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.openaiUrl = openaiUrl;
        this.apiKey = apiKey;
        this.modeloPorDefecto = modeloPorDefecto;
    }

    @Override
    public String getNombre() {
        return "gemini";
    }

    @Override
    public RespuestaIa generar(String promptSistema, String promptUsuario) {
        long inicio = System.currentTimeMillis();

        Map<String, Object> cuerpo = Map.of(
                "model", modeloPorDefecto,
                "messages", List.of(
                        Map.of("role", "system", "content", promptSistema),
                        Map.of("role", "user", "content", promptUsuario)
                ),
                "temperature", 0.2
        );

        String urlBase = openaiUrl != null ? openaiUrl.trim() : "";
        String urlEndpoint = urlBase.endsWith("/chat/completions")
                ? urlBase
                : urlBase.replaceAll("/+$", "") + "/chat/completions";

        String respuestaCruda;
        try {
            respuestaCruda = ejecutarPost(urlEndpoint, cuerpo);
        } catch (HttpClientErrorException.NotFound ex) {
            if (!urlEndpoint.equals(urlBase) && !urlBase.isBlank()) {
                respuestaCruda = ejecutarPost(urlBase, cuerpo);
            } else {
                throw ex;
            }
        }

        long milisegundos = Math.max(1L, System.currentTimeMillis() - inicio);
        return parsearRespuesta(respuestaCruda, milisegundos);
    }

    private String ejecutarPost(String url, Map<String, Object> cuerpo) {
        RestClient.RequestBodySpec spec = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON);

        if (apiKey != null && !apiKey.isBlank()) {
            spec = spec.header("Authorization", "Bearer " + apiKey);
        }

        return spec.body(cuerpo)
                .retrieve()
                .body(String.class);
    }

    private RespuestaIa parsearRespuesta(String respuestaCruda, long milisegundos) {
        if (respuestaCruda == null || respuestaCruda.isBlank()) {
            throw new IllegalStateException("Respuesta vacía del proveedor de IA");
        }

        JsonNode root = objectMapper.readTree(respuestaCruda);
        String contenido = null;

        if (root.has("choices") && root.get("choices").isArray() && !root.get("choices").isEmpty()) {
            JsonNode messageNode = root.get("choices").get(0).path("message");
            if (messageNode.has("content") && !messageNode.get("content").isNull()) {
                contenido = messageNode.get("content").asText();
            }
        } else if (root.has("content") && !root.get("content").isNull()) {
            contenido = root.get("content").asText();
        } else if (root.has("mensaje") || root.has("respuesta")) {
            contenido = respuestaCruda;
        }

        if (contenido == null || contenido.isBlank()) {
            throw new IllegalStateException("El proveedor de IA no devolvió contenido");
        }

        String modeloUsado = root.has("model") && !root.get("model").isNull()
                ? root.get("model").asText()
                : modeloPorDefecto;

        JsonNode usage = root.path("usage");
        int tokensEntrada = usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt(0) : 0;
        int tokensSalida = usage.has("completion_tokens") ? usage.get("completion_tokens").asInt(0) : 0;

        double costoEstimadoUsd = Math.round(((tokensEntrada * 0.00000015) + (tokensSalida * 0.0000006)) * 1_000_000.0) / 1_000_000.0;

        FacturaIaDTO factura = new FacturaIaDTO(
                getNombre(),
                modeloUsado,
                tokensEntrada,
                tokensSalida,
                costoEstimadoUsd,
                milisegundos
        );

        return new RespuestaIa(contenido, factura);
    }
}
