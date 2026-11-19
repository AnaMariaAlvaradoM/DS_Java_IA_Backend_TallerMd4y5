package com.devsenior.sillon.ia.service.openai;

import com.devsenior.sillon.ia.config.FabricaHttp;
import com.devsenior.sillon.ia.config.IaProperties;
import com.devsenior.sillon.ia.excepcion.ProveedorIAException;
import com.devsenior.sillon.ia.model.RespuestaIA;
import com.devsenior.sillon.ia.service.ProveedorIA;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Proveedor principal: Chat Completions con formato OpenAI, apuntando a Gemini.
 * La URL sale de sillon.ia.openai.url: nunca va fija en el código (el calificador la cambia).
 */
@Component
@Order(1)
public class ProveedorOpenAi implements ProveedorIA {

    private static final int MAX_TOKENS = 1024;

    private final IaProperties.Proveedor config;
    private final RestClient restClient;

    public ProveedorOpenAi(IaProperties propiedades, FabricaHttp fabricaHttp) {
        this.config = propiedades.openai();
        this.restClient = fabricaHttp.crear();
    }

    @Override
    public String nombre() {
        return "openai";
    }

    @Override
    public RespuestaIA completar(String instrucciones, String mensaje) {
        if (!config.tieneLlave()) {
            throw new ProveedorIAException(nombre(), "no tiene llave configurada", true);
        }
        OpenAiFormato.Peticion peticion = new OpenAiFormato.Peticion(
                config.modelo(),
                List.of(new OpenAiFormato.Mensaje("system", instrucciones),
                        new OpenAiFormato.Mensaje("user", mensaje)),
                MAX_TOKENS,
                0.0);

        long inicio = System.currentTimeMillis();
        OpenAiFormato.Respuesta respuesta = enviar(peticion);
        long milisegundos = System.currentTimeMillis() - inicio;

        String texto = extraerTexto(respuesta);
        int tokensEntrada = respuesta.usage() == null || respuesta.usage().promptTokens() == null
                ? 0 : respuesta.usage().promptTokens();
        int tokensSalida = respuesta.usage() == null || respuesta.usage().completionTokens() == null
                ? 0 : respuesta.usage().completionTokens();
        return new RespuestaIA(texto, nombre(), config.modelo(), tokensEntrada, tokensSalida,
                config.calcularCosto(tokensEntrada, tokensSalida), milisegundos);
    }

    private OpenAiFormato.Respuesta enviar(OpenAiFormato.Peticion peticion) {
        try {
            OpenAiFormato.Respuesta respuesta = restClient.post()
                    .uri(config.urlSinBarraFinal() + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.llave())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(peticion)
                    .retrieve()
                    .body(OpenAiFormato.Respuesta.class);
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

    private String extraerTexto(OpenAiFormato.Respuesta respuesta) {
        if (respuesta.choices() == null || respuesta.choices().isEmpty()
                || respuesta.choices().get(0).message() == null) {
            throw new ProveedorIAException(nombre(), "respuesta sin opciones", false);
        }
        String texto = respuesta.choices().get(0).message().content();
        if (texto == null || texto.isBlank()) {
            throw new ProveedorIAException(nombre(), "respuesta sin texto", false);
        }
        return texto;
    }
}
