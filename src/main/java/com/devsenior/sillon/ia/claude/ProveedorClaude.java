package com.devsenior.sillon.ia.claude;

import com.devsenior.sillon.ia.FabricaHttp;
import com.devsenior.sillon.ia.IaProperties;
import com.devsenior.sillon.ia.ProveedorIA;
import com.devsenior.sillon.ia.ProveedorIAException;
import com.devsenior.sillon.ia.RespuestaIA;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@Order(2)
public class ProveedorClaude implements ProveedorIA {

    private static final String VERSION_API = "2023-06-01";

    private final IaProperties.Proveedor config;
    private final int maxTokens;
    private final RestClient restClient;

    public ProveedorClaude(RestClient.Builder builder, IaProperties propiedades) {
        this.config = propiedades.claude();
        this.maxTokens = propiedades.maxTokens();
        this.restClient = builder
                .baseUrl(config.url())
                .defaultHeader("x-api-key", config.llave())
                .defaultHeader("anthropic-version", VERSION_API)
                .requestFactory(FabricaHttp.conTimeout(propiedades.timeoutSegundos()))
                .build();
    }

    @Override
    public String nombre() {
        return "claude";
    }

    @Override
    public RespuestaIA completar(String instrucciones, String mensaje) {
        ClaudeFormato.Peticion peticion = new ClaudeFormato.Peticion(
                config.modelo(),
                maxTokens,
                instrucciones,
                List.of(new ClaudeFormato.Mensaje("user", mensaje)),
                0.0,
                new ClaudeFormato.Pensamiento("disabled"));

        long inicio = System.currentTimeMillis();
        ClaudeFormato.Respuesta respuesta = enviar(peticion);
        long milisegundos = System.currentTimeMillis() - inicio;

        String texto = extraerTexto(respuesta);
        int tokensEntrada = respuesta.usage() == null ? 0
                : respuesta.usage().inputTokens() + respuesta.usage().cacheReadInputTokens();
        int tokensSalida = respuesta.usage() == null ? 0 : respuesta.usage().outputTokens();

        return new RespuestaIA(texto, nombre(), config.modelo(), tokensEntrada, tokensSalida,
                config.calcularCosto(tokensEntrada, tokensSalida), milisegundos);
    }

    private ClaudeFormato.Respuesta enviar(ClaudeFormato.Peticion peticion) {
        try {
            return restClient.post()
                    .uri("/v1/messages")
                    .body(peticion)
                    .retrieve()
                    .body(ClaudeFormato.Respuesta.class);
        } catch (RestClientResponseException ex) {
            throw ProveedorIAException.desdeEstadoHttp(nombre(), ex.getStatusCode().value());
        } catch (ResourceAccessException ex) {
            throw new ProveedorIAException(nombre(), "sin conexión o tiempo agotado", false);
        }
    }

    private String extraerTexto(ClaudeFormato.Respuesta respuesta) {
        if (respuesta == null || respuesta.content() == null) {
            throw new ProveedorIAException(nombre(), "respuesta sin contenido", false);
        }
        String texto = respuesta.content().stream()
                .filter(bloque -> "text".equals(bloque.type()))
                .map(ClaudeFormato.Bloque::text)
                .collect(Collectors.joining());
        if (texto.isBlank()) {
            throw new ProveedorIAException(nombre(), "respuesta vacía", false);
        }
        return texto.strip();
    }
}
