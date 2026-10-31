package com.devsenior.sillon.ia.openai;

import com.devsenior.sillon.ia.FabricaHttp;
import com.devsenior.sillon.ia.IaProperties;
import com.devsenior.sillon.ia.ProveedorIA;
import com.devsenior.sillon.ia.ProveedorIAException;
import com.devsenior.sillon.ia.RespuestaIA;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@Order(1)
public class ProveedorOpenAi implements ProveedorIA {

    private final IaProperties.Proveedor config;
    private final int maxTokens;
    private final RestClient restClient;

    public ProveedorOpenAi(RestClient.Builder builder, IaProperties propiedades) {
        this.config = propiedades.openai();
        this.maxTokens = propiedades.maxTokens();
        this.restClient = builder
                .baseUrl(config.url())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + propiedades.gemini().llave())
                .requestFactory(FabricaHttp.conTimeout(propiedades.timeoutSegundos()))
                .build();
    }

    @Override
    public String nombre() {
        return "openai";
    }

    @Override
    public RespuestaIA completar(String instrucciones, String mensaje) {
        OpenAiFormato.Peticion peticion = new OpenAiFormato.Peticion(
                config.modelo(),
                List.of(new OpenAiFormato.Mensaje("system", instrucciones),
                        new OpenAiFormato.Mensaje("user", mensaje)),
                maxTokens,
                0.0);

        long inicio = System.currentTimeMillis();
        OpenAiFormato.Respuesta respuesta = enviar(peticion);
        long milisegundos = System.currentTimeMillis() - inicio;

        String texto = extraerTexto(respuesta);
        int tokensEntrada = respuesta.usage() == null ? 0 : respuesta.usage().promptTokens();
        int tokensSalida = respuesta.usage() == null ? 0 : respuesta.usage().completionTokens();

        return new RespuestaIA(texto, nombre(), config.modelo(), tokensEntrada, tokensSalida,
                config.calcularCosto(tokensEntrada, tokensSalida), milisegundos);
    }

    private OpenAiFormato.Respuesta enviar(OpenAiFormato.Peticion peticion) {
        try {
            return restClient.post()
                    .uri("/chat/completions")
                    .body(peticion)
                    .retrieve()
                    .body(OpenAiFormato.Respuesta.class);
        } catch (RestClientResponseException ex) {
            throw ProveedorIAException.desdeEstadoHttp(nombre(), ex.getStatusCode().value());
        } catch (ResourceAccessException ex) {
            throw new ProveedorIAException(nombre(), "sin conexión o tiempo agotado", false);
        }
    }

    private String extraerTexto(OpenAiFormato.Respuesta respuesta) {
        if (respuesta == null || respuesta.choices() == null || respuesta.choices().isEmpty()) {
            throw new ProveedorIAException(nombre(), "respuesta sin opciones", false);
        }
        String texto = respuesta.choices().get(0).message().content();
        if (texto == null || texto.isBlank()) {
            throw new ProveedorIAException(nombre(), "respuesta vacía", false);
        }
        return texto.strip();
    }
}
