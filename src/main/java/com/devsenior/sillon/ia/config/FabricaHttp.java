package com.devsenior.sillon.ia.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Fabrica los RestClient de los proveedores con tiempo máximo de conexión y de lectura.
 * Sin tiempo máximo, un proveedor colgado bloquearía la petición y el fallback nunca avanzaría.
 */
@Component
public class FabricaHttp {

    private final RestClient.Builder builder;
    private final Duration timeout;

    public FabricaHttp(RestClient.Builder builder, IaProperties propiedades) {
        this.builder = builder;
        this.timeout = Duration.ofSeconds(propiedades.timeoutSegundos());
    }

    public RestClient crear() {
        HttpClient cliente = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
        fabrica.setReadTimeout(timeout);
        return builder.clone().requestFactory(fabrica).build();
    }
}
