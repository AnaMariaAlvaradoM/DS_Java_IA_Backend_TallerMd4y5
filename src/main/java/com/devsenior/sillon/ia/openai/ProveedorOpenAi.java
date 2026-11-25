package com.devsenior.sillon.ia.openai;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
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
@Order(1)
public class ProveedorOpenAi implements ProveedorIa {

    private final IaPropiedades.ProveedorConfig config;
    private final int maxTokens;
    private final RestClient restClient;

    public ProveedorOpenAi(RestClient.Builder builder, IaPropiedades propiedades) {
        this.config = propiedades.openai();
        this.maxTokens = propiedades.maxTokens() > 0 ? propiedades.maxTokens() : 1024;

        String baseUrl = config != null && config.url() != null && !config.url().isBlank()
                ? config.url()
                : "https://generativelanguage.googleapis.com/v1beta/openai";

        String llave = config != null && config.llave() != null && !config.llave().isBlank()
                ? config.llave()
                : (propiedades.gemini() != null ? propiedades.gemini().llave() : "sin-llave");

        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + llave)
                .requestFactory(FabricaHttp.conTimeout(propiedades.timeoutSegundos()))
                .build();
    }

    @Override
    public String nombre() {
        return "openai";
    }

    @Override
    public RespuestaIa completar(String instrucciones, String mensaje) {
        String modelo = config != null && config.modelo() != null && !config.modelo().isBlank()
                ? config.modelo()
                : "gemini-2.0-flash";

        OpenAiFormato.Peticion peticion = new OpenAiFormato.Peticion(
                modelo,
                List.of(
                        new OpenAiFormato.Mensaje("system", instrucciones),
                        new OpenAiFormato.Mensaje("user", mensaje)),
                maxTokens,
                0.0);

        long inicio = System.currentTimeMillis();
        OpenAiFormato.Respuesta respuesta = enviar(peticion);
        long milisegundos = System.currentTimeMillis() - inicio;

        String texto = extraerTexto(respuesta);
        int tokensEntrada = respuesta.usage() == null ? 0 : respuesta.usage().promptTokens();
        int tokensSalida = respuesta.usage() == null ? 0 : respuesta.usage().completionTokens();

        FacturaIa factura = new FacturaIa(
                nombre(),
                modelo,
                tokensEntrada,
                tokensSalida,
                config != null ? config.calcularCosto(tokensEntrada, tokensSalida) : null,
                milisegundos);

        return new RespuestaIa(texto, factura);
    }

    private OpenAiFormato.Respuesta enviar(OpenAiFormato.Peticion peticion) {
        try {
            return restClient.post()
                    .uri("/chat/completions")
                    .body(peticion)
                    .retrieve()
                    .body(OpenAiFormato.Respuesta.class);
        } catch (RestClientResponseException ex) {
            throw ProveedorIaException.desdeEstadoHttp(nombre(), ex.getStatusCode().value());
        } catch (ResourceAccessException ex) {
            throw new ProveedorIaException(nombre(), "sin conexión o tiempo agotado", false);
        }
    }

    private String extraerTexto(OpenAiFormato.Respuesta respuesta) {
        if (respuesta == null || respuesta.choices() == null || respuesta.choices().isEmpty()) {
            throw new ProveedorIaException(nombre(), "respuesta sin opciones", false);
        }
        String texto = respuesta.choices().get(0).message().content();
        if (texto == null || texto.isBlank()) {
            throw new ProveedorIaException(nombre(), "respuesta vacía", false);
        }
        return texto.strip();
    }
}
