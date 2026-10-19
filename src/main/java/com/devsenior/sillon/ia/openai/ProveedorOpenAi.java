package com.devsenior.sillon.ia.openai;

import com.devsenior.sillon.ia.FabricaHttp;
import com.devsenior.sillon.ia.IaProperties;
import com.devsenior.sillon.ia.ProveedorIA;
import com.devsenior.sillon.ia.ProveedorIAException;
import com.devsenior.sillon.ia.RespuestaIA;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;

@Component
@Order(1)
public class ProveedorOpenAi implements ProveedorIA {

    private final IaProperties.Proveedor config;
    private final int maxTokens;
    private final RestClient restClient;

    public ProveedorOpenAi(RestClient.Builder builder, IaProperties propiedades) {
        this.config = propiedades.openai();
        this.maxTokens = propiedades.maxTokens();
        this.restClient = builder.clone()
                .requestFactory(FabricaHttp.conTimeout(propiedades.timeoutSegundos()))
                .build();
    }

    @Override
    public String nombre() {
        return "openai";
    }

    @Override
    public RespuestaIA completar(String instrucciones, String mensaje) {
        if (config.llave() == null || config.llave().isBlank() || "REEMPLAZAR".equals(config.llave())) {
            throw new ProveedorIAException(nombre(), "falta configurar la llave", true);
        }
        OpenAiFormato.Peticion peticion = new OpenAiFormato.Peticion(
                config.modelo(),
                List.of(new OpenAiFormato.Mensaje("system", instrucciones),
                        new OpenAiFormato.Mensaje("user", mensaje)),
                maxTokens);

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
                    .uri(config.url())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.llave())
                    .body(peticion)
                    .retrieve()
                    .body(OpenAiFormato.Respuesta.class);
        } catch (RestClientResponseException ex) {
            throw ProveedorIAException.desdeEstadoHttp(nombre(), ex.getStatusCode().value());
        } catch (ResourceAccessException ex) {
            throw new ProveedorIAException(nombre(), "sin conexión o tiempo agotado", false);
        } catch (RestClientException ex) {
            throw new ProveedorIAException(nombre(), "respuesta HTTP no utilizable", false);
        }
    }

    private String extraerTexto(OpenAiFormato.Respuesta respuesta) {
        if (respuesta == null || respuesta.choices() == null || respuesta.choices().isEmpty()) {
            throw new ProveedorIAException(nombre(), "respuesta sin opciones", false);
        }
        var opcion = respuesta.choices().get(0);
        if (opcion == null || opcion.message() == null) {
            throw new ProveedorIAException(nombre(), "respuesta sin mensaje", false);
        }
        String texto = opcion.message().content();
        if (texto == null || texto.isBlank()) {
            throw new ProveedorIAException(nombre(), "respuesta vacía", false);
        }
        return texto.strip();
    }
}
