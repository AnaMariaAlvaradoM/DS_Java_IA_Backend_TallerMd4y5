
package com.devsenior.sillon.ia;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;

import org.springframework.stereotype.Component;

@Component
public class FabricaHttpIA {

    private static final Duration TIEMPO_CONEXION =
            Duration.ofSeconds(10);

    private static final Duration TIEMPO_RESPUESTA =
            Duration.ofSeconds(30);

    private final HttpClient cliente;

    public FabricaHttpIA() {
        this.cliente = HttpClient.newBuilder()
                .connectTimeout(TIEMPO_CONEXION)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public HttpClient cliente() {
        return cliente;
    }

    public HttpRequest.Builder prepararSolicitud(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIEMPO_RESPUESTA)
                .header("Content-Type", "application/json");
    }
}
