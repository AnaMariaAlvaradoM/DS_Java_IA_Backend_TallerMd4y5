package com.devsenior.sillon.ia.service.claude;

import com.devsenior.sillon.ia.config.FabricaHttp;
import com.devsenior.sillon.ia.config.IaProperties;
import com.devsenior.sillon.ia.excepcion.ProveedorIAException;
import com.devsenior.sillon.ia.model.RespuestaIA;
import com.devsenior.sillon.ia.service.ProveedorIA;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Proveedor de respaldo con formato Claude. Queda configurado aunque no haya llave:
 * así el fallback se ve en los logs (error de configuración) y termina en 503.
 */
@Component
@Order(2)
public class ProveedorClaude implements ProveedorIA {

    private static final int MAX_TOKENS = 1024;
    private static final String VERSION_API = "2023-06-01";

    private final IaProperties.Proveedor config;
    private final RestClient restClient;

    public ProveedorClaude(IaProperties propiedades, FabricaHttp fabricaHttp) {
        this.config = propiedades.claude();
        this.restClient = fabricaHttp.crear();
    }

    @Override
    public String nombre() {
        return "claude";
    }

    @Override
    public RespuestaIA completar(String instrucciones, String mensaje) {
        if (!config.tieneLlave()) {
            throw new ProveedorIAException(nombre(), "no tiene llave configurada", true);
        }
        ClaudeFormato.Peticion peticion = new ClaudeFormato.Peticion(config.modelo(), MAX_TOKENS, instrucciones,
                List.of(new ClaudeFormato.Mensaje("user", mensaje)), 0.0);

        long inicio = System.currentTimeMillis();
        ClaudeFormato.Respuesta respuesta = enviar(peticion);
        long milisegundos = System.currentTimeMillis() - inicio;

        String texto = respuesta.content() == null ? "" : respuesta.content().stream()
                .filter(bloque -> "text".equals(bloque.type()))
                .map(ClaudeFormato.Bloque::text)
                .filter(Objects::nonNull)
                .collect(Collectors.joining());
        if (texto.isBlank()) {
            throw new ProveedorIAException(nombre(), "respuesta sin texto", false);
        }
        ClaudeFormato.Uso uso = respuesta.usage();
        int tokensEntrada = uso == null ? 0
                : valor(uso.inputTokens()) + valor(uso.cacheReadInputTokens());
        int tokensSalida = uso == null ? 0 : valor(uso.outputTokens());
        return new RespuestaIA(texto, nombre(), config.modelo(), tokensEntrada, tokensSalida,
                config.calcularCosto(tokensEntrada, tokensSalida), milisegundos);
    }

    private ClaudeFormato.Respuesta enviar(ClaudeFormato.Peticion peticion) {
        try {
            ClaudeFormato.Respuesta respuesta = restClient.post()
                    .uri(config.urlSinBarraFinal() + "/v1/messages")
                    .header("x-api-key", config.llave())
                    .header("anthropic-version", VERSION_API)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(peticion)
                    .retrieve()
                    .body(ClaudeFormato.Respuesta.class);
            if (respuesta == null) {
                throw new ProveedorIAException(nombre(), "respuesta vacía", false);
            }
            return respuesta;
        } catch (RestClientResponseException ex) {
            throw ProveedorIAException.desdeCodigoHttp(nombre(), ex.getStatusCode().value());
        } catch (RestClientException ex) {
            throw new ProveedorIAException(nombre(), "sin conexión o tiempo agotado", false);
        }
    }

    private int valor(Integer numero) {
        return numero == null ? 0 : numero;
    }
}
