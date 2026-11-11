package com.devsenior.sillon.ia;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;

public final class FabricaHttp {

    private FabricaHttp() {
    }

    public static ClientHttpRequestFactory conTimeout(int segundos) {
        Duration limite = Duration.ofSeconds(segundos);
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(limite)
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(limite);
        return fabrica;
    }
}
