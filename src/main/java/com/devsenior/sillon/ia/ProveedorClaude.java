package com.devsenior.sillon.ia;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class ProveedorClaude implements ProveedorIa {
    private final RestClient cliente;
    private final ObjectMapper json;
    private final IaConfiguracion.Proveedor configuracion;

    public ProveedorClaude(RestClient.Builder fabrica, ObjectMapper json, IaConfiguracion ia) {
        configuracion = ia.claude();
        cliente = fabrica.build();
        this.json = json;
    }

    @Override public String nombre() { return "Claude"; }

    @Override public RespuestaIa generar(String prompt) {
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("La llamada IA no puede ejecutarse en una transacción JPA");
        if (configuracion.llave() == null || configuracion.llave().isBlank()
                || configuracion.llave().endsWith("_placeholder"))
            throw new FalloProveedorIa(FalloProveedorIa.Tipo.CONFIGURACION);
        long inicio = System.nanoTime();
        try {
            String cuerpo = cliente.post().uri(configuracion.url().replaceAll("/+$", "") + "/v1/messages")
                    .header("x-api-key", configuracion.llave())
                    .header("anthropic-version", "2023-06-01")
                    .body(Map.of("model", configuracion.modelo(), "max_tokens", 1024,
                            "messages", List.of(Map.of("role", "user", "content", prompt))))
                    .retrieve().body(String.class);
            JsonNode raiz = json.readTree(cuerpo);
            JsonNode contenido = raiz.path("content").path(0).path("text");
            if (!contenido.isTextual() || contenido.asText().isBlank())
                throw new FalloProveedorIa(FalloProveedorIa.Tipo.RESPUESTA_INVALIDA);
            JsonNode uso = raiz.path("usage");
            return new RespuestaIa(contenido.asText(), FacturaIa.crear(nombre(), configuracion.modelo(),
                    entero(uso.path("input_tokens")), entero(uso.path("output_tokens")),
                    (System.nanoTime() - inicio) / 1_000_000, configuracion));
        } catch (RestClientException error) {
            throw ErroresProveedorIa.clasificar(error);
        } catch (FalloProveedorIa error) {
            throw error;
        } catch (RuntimeException error) {
            throw new FalloProveedorIa(FalloProveedorIa.Tipo.RESPUESTA_INVALIDA);
        }
    }

    private Integer entero(JsonNode nodo) { return nodo.isIntegralNumber() && nodo.asInt() >= 0 ? nodo.asInt() : null; }
}
