package com.devsenior.sillon.ia.proveedor;

import com.devsenior.sillon.ia.dto.FacturaIa;
import com.devsenior.sillon.ia.dto.RespuestaIa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@Order(1)
public class ProveedorOpenAi implements ProveedorIa {

    private static final Logger log = LoggerFactory.getLogger(ProveedorOpenAi.class);
    private static final String MODELO = "gemini-2.5-flash";

    private final RestClient restClient;
    private final String urlBase;
    private final String llave;

    public ProveedorOpenAi(
            @Value("${sillon.ia.openai.url}") String urlBase,
            @Value("${sillon.ia.gemini.llave}") String llave
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(20000);

        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.urlBase = urlBase.endsWith("/") ? urlBase : urlBase + "/";
        this.llave = llave;
    }

    @Override
    public String nombre() {
        return "openai-gemini";
    }

    @Override
    @SuppressWarnings("unchecked")
    public RespuestaIa generar(String instruccionesSistema, String mensajeUsuario) {
        long inicio = System.currentTimeMillis();
        try {
            Map<String, Object> body = Map.of(
                    "model", MODELO,
                    "temperature", 0.3,
                    "messages", List.of(
                            Map.of("role", "system", "content", instruccionesSistema),
                            Map.of("role", "user", "content", mensajeUsuario)
                    )
            );

            Map<String, Object> respuesta = restClient.post()
                    .uri(urlBase + "chat/completions")
                    .header("Authorization", "Bearer " + llave)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (respuesta == null || !respuesta.containsKey("choices")) {
                throw new IllegalStateException("Respuesta vacía del proveedor OpenAI/Gemini");
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) respuesta.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new IllegalStateException("Sin choices en la respuesta de IA");
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String contenido = message != null ? String.valueOf(message.get("content")) : "";

            int tokensEntrada = 0;
            int tokensSalida = 0;
            if (respuesta.get("usage") instanceof Map<?, ?> usage) {
                Object pt = usage.get("prompt_tokens");
                Object ct = usage.get("completion_tokens");
                tokensEntrada = pt instanceof Number n ? n.intValue() : 0;
                tokensSalida = ct instanceof Number n ? n.intValue() : 0;
            }

            long ms = System.currentTimeMillis() - inicio;
            double costo = (tokensEntrada * 0.00000015) + (tokensSalida * 0.0000006);

            return new RespuestaIa(
                    contenido,
                    new FacturaIa(nombre(), MODELO, tokensEntrada, tokensSalida, costo, ms)
            );
        } catch (Exception e) {
            log.error("Fallo en proveedor primario {}: {}", nombre(), e.getMessage());
            throw new RuntimeException("Error en proveedor " + nombre(), e);
        }
    }
}

