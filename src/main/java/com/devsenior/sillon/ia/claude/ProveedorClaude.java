package com.devsenior.sillon.ia.claude;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.devsenior.sillon.ia.FabricaHttp;
import com.devsenior.sillon.ia.FacturaIa;
import com.devsenior.sillon.ia.IaPropiedades;
import com.devsenior.sillon.ia.ProveedorIa;
import com.devsenior.sillon.ia.ProveedorIaException;
import com.devsenior.sillon.ia.RespuestaIa;

@Component
@Order(2)
public class ProveedorClaude implements ProveedorIa {

    private static final String VERSION_API = "2023-06-01";

    private final IaPropiedades.ProveedorConfig config;
    private final int maxTokens;
    private final RestClient restClient;

    public ProveedorClaude(RestClient.Builder builder, IaPropiedades propiedades) {
        this.config = propiedades.claude();
        this.maxTokens = propiedades.maxTokens() > 0 ? propiedades.maxTokens() : 1024;

        String baseUrl = config != null && config.url() != null && !config.url().isBlank()
                ? config.url()
                : "https://api.anthropic.com";

        String llave = config != null && config.llave() != null && !config.llave().isBlank()
                ? config.llave()
                : "sin-llave";

        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", llave)
                .defaultHeader("anthropic-version", VERSION_API)
                .requestFactory(FabricaHttp.conTimeout(propiedades.timeoutSegundos()))
                .build();
    }

    @Override
    public String nombre() {
        return "claude";
    }

    @Override
    public RespuestaIa completar(String instrucciones, String mensaje) {
        String modelo = config != null && config.modelo() != null && !config.modelo().isBlank()
                ? config.modelo()
                : "claude-3-haiku-20240307";

        ClaudeFormato.Peticion peticion = new ClaudeFormato.Peticion(
                modelo,
                maxTokens,
                instrucciones,
                List.of(new ClaudeFormato.Mensaje("user", mensaje)),
                0.0);

        long inicio = System.currentTimeMillis();
        ClaudeFormato.Respuesta respuesta = enviar(peticion);
        long milisegundos = System.currentTimeMillis() - inicio;

        String texto = extraerTexto(respuesta);
        int tokensEntrada = respuesta.usage() == null ? 0 : respuesta.usage().inputTokens();
        int tokensSalida = respuesta.usage() == null ? 0 : respuesta.usage().outputTokens();

        FacturaIa factura = new FacturaIa(
                nombre(),
                modelo,
                tokensEntrada,
                tokensSalida,
                config != null ? config.calcularCosto(tokensEntrada, tokensSalida) : null,
                milisegundos);

        return new RespuestaIa(texto, factura);
    }

    private ClaudeFormato.Respuesta enviar(ClaudeFormato.Peticion peticion) {
        try {
            return restClient.post()
                    .uri("/v1/messages")
                    .body(peticion)
                    .retrieve()
                    .body(ClaudeFormato.Respuesta.class);
        } catch (RestClientResponseException ex) {
            throw ProveedorIaException.desdeEstadoHttp(nombre(), ex.getStatusCode().value());
        } catch (ResourceAccessException ex) {
            throw new ProveedorIaException(nombre(), "sin conexión o tiempo agotado", false);
        }
    }

    private String extraerTexto(ClaudeFormato.Respuesta respuesta) {
        if (respuesta == null || respuesta.content() == null) {
            throw new ProveedorIaException(nombre(), "respuesta sin contenido", false);
        }
        String texto = respuesta.content().stream()
                .filter(bloque -> "text".equals(bloque.type()))
                .map(ClaudeFormato.Bloque::text)
                .collect(Collectors.joining());
        if (texto.isBlank()) {
            throw new ProveedorIaException(nombre(), "respuesta vacía", false);
        }
        return texto.strip();
    }
}
