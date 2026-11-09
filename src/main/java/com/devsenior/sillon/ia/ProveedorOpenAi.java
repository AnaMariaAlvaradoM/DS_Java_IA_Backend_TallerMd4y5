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
public class ProveedorOpenAi implements ProveedorIa {
    private final RestClient cliente;
    private final ObjectMapper json;
    private final IaConfiguracion.Proveedor configuracion;

    public ProveedorOpenAi(RestClient.Builder fabrica, ObjectMapper json, IaConfiguracion ia) {
        configuracion = ia.openai();
        cliente = fabrica.build();
        this.json = json;
    }

    @Override public String nombre() { return "Gemini"; }

    @Override public RespuestaIa generar(PeticionIa peticion) {
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("La llamada IA no puede ejecutarse en una transacción JPA");
        if (configuracion.llave() == null || configuracion.llave().isBlank())
            throw new FalloProveedorIa(FalloProveedorIa.Tipo.CONFIGURACION);
        long inicio = System.nanoTime();
        try {
            String cuerpo = cliente.post().uri(configuracion.url().replaceAll("/+$", "") + "/chat/completions")
                    .header("Authorization", "Bearer " + configuracion.llave())
                    .body(Map.of("model", configuracion.modelo(),
                            "messages", mensajes(peticion),
                            "response_format", Map.of("type", "json_object")))
                    .retrieve().body(String.class);
            JsonNode raiz = json.readTree(cuerpo);
            JsonNode contenido = raiz.path("choices").path(0).path("message").path("content");
            if (!contenido.isTextual() || contenido.asText().isBlank())
                throw new FalloProveedorIa(FalloProveedorIa.Tipo.RESPUESTA_INVALIDA);
            JsonNode uso = raiz.path("usage");
            return new RespuestaIa(contenido.asText(), FacturaIa.crear(nombre(), configuracion.modelo(),
                    entero(uso.path("prompt_tokens")), entero(uso.path("completion_tokens")),
                    (System.nanoTime() - inicio) / 1_000_000, configuracion));
        } catch (RestClientException error) {
            throw ErroresProveedorIa.clasificar(error);
        } catch (FalloProveedorIa error) {
            throw error;
        } catch (RuntimeException error) {
            throw new FalloProveedorIa(FalloProveedorIa.Tipo.RESPUESTA_INVALIDA);
        }
    }

    private List<Map<String, String>> mensajes(PeticionIa peticion) {
        Map<String, String> usuario = Map.of("role", "user", "content", peticion.usuario());
        if (peticion.sistema() == null || peticion.sistema().isBlank()) return List.of(usuario);
        return List.of(Map.of("role", "system", "content", peticion.sistema()), usuario);
    }

    private Integer entero(JsonNode nodo) { return nodo.isIntegralNumber() && nodo.asInt() >= 0 ? nodo.asInt() : null; }
}
