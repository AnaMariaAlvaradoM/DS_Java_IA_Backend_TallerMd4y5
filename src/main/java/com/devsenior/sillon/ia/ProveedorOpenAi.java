package com.devsenior.sillon.ia;

import java.util.List;
import java.util.Map;

import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Proveedor principal: habla el formato OpenAI (chat completions), que es el que usa Gemini. */
@Component
@Order(1)
public class ProveedorOpenAi implements ProveedorIa {

    // Precios aproximados de Gemini Flash por millón de tokens (Clase 18)
    private static final double USD_MILLON_ENTRADA = 0.30;
    private static final double USD_MILLON_SALIDA = 2.50;

    private final RestClient restClient;
    private final IaPropiedades.Proveedor config;

    public ProveedorOpenAi(RestClient iaRestClient, IaPropiedades propiedades) {
        this.restClient = iaRestClient;
        this.config = propiedades.openai();
    }

    @Override
    public String nombre() {
        return "gemini";
    }

    @Override
    @SuppressWarnings("unchecked")
    public RespuestaIa generar(String instrucciones, String mensajeUsuario) {
        Map<String, Object> cuerpo = Map.of(
                "model", config.modelo(),
                "messages", List.of(
                        Map.of("role", "system", "content", instrucciones),
                        Map.of("role", "user", "content", mensajeUsuario)),
                "response_format", Map.of("type", "json_object"));
        long inicio = System.currentTimeMillis();
        Map<String, Object> respuesta;
        try {
            respuesta = restClient.post()
                    .uri(config.url())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + config.llave())
                    .body(cuerpo)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            int codigo = e.getStatusCode().value();
            throw new ErrorProveedorIa("HTTP " + codigo, ErrorProveedorIa.esErrorDeConfiguracion(codigo));
        } catch (RestClientException e) {
            throw new ErrorProveedorIa("Sin respuesta: " + e.getMessage(), false);
        }
        long milisegundos = System.currentTimeMillis() - inicio;
        try {
            Map<String, Object> mensaje = (Map<String, Object>) ((Map<String, Object>) ((List<Object>) respuesta
                    .get("choices")).get(0)).get("message");
            String texto = String.valueOf(mensaje.get("content"));
            Map<String, Object> uso = (Map<String, Object>) respuesta.getOrDefault("usage", Map.of());
            int entrada = numero(uso.get("prompt_tokens"));
            int salida = numero(uso.get("completion_tokens"));
            double costo = (entrada * USD_MILLON_ENTRADA + salida * USD_MILLON_SALIDA) / 1_000_000;
            return new RespuestaIa(texto, nombre(), config.modelo(), entrada, salida, milisegundos, costo);
        } catch (RuntimeException e) {
            throw new ErrorProveedorIa("Respuesta con formato inesperado", false);
        }
    }

    static int numero(Object valor) {
        return valor instanceof Number n ? n.intValue() : 0;
    }
}
