package com.devsenior.sillon.ia;

import java.util.List;
import java.util.Map;

import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Proveedor de respaldo con el formato de la API de Claude (Messages API). */
@Component
@Order(2)
public class ProveedorClaude implements ProveedorIa {

    private static final double USD_MILLON_ENTRADA = 1.00;
    private static final double USD_MILLON_SALIDA = 5.00;

    private final RestClient restClient;
    private final IaPropiedades.Proveedor config;

    public ProveedorClaude(RestClient iaRestClient, IaPropiedades propiedades) {
        this.restClient = iaRestClient;
        this.config = propiedades.claude();
    }

    @Override
    public String nombre() {
        return "claude";
    }

    @Override
    @SuppressWarnings("unchecked")
    public RespuestaIa generar(String instrucciones, String mensajeUsuario) {
        Map<String, Object> cuerpo = Map.of(
                "model", config.modelo(),
                "max_tokens", 600,
                "system", instrucciones,
                "messages", List.of(Map.of("role", "user", "content", mensajeUsuario)));
        long inicio = System.currentTimeMillis();
        Map<String, Object> respuesta;
        try {
            respuesta = restClient.post()
                    .uri(config.url())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", config.llave())
                    .header("anthropic-version", "2023-06-01")
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
            Map<String, Object> bloque = (Map<String, Object>) ((List<Object>) respuesta.get("content")).get(0);
            String texto = String.valueOf(bloque.get("text"));
            Map<String, Object> uso = (Map<String, Object>) respuesta.getOrDefault("usage", Map.of());
            int entrada = ProveedorOpenAi.numero(uso.get("input_tokens"));
            int salida = ProveedorOpenAi.numero(uso.get("output_tokens"));
            double costo = (entrada * USD_MILLON_ENTRADA + salida * USD_MILLON_SALIDA) / 1_000_000;
            return new RespuestaIa(texto, nombre(), config.modelo(), entrada, salida, milisegundos, costo);
        } catch (RuntimeException e) {
            throw new ErrorProveedorIa("Respuesta con formato inesperado", false);
        }
    }
}
