package com.devsenior.sillon.ia;

import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ClienteHttpIa {
    public RestClient crear(String baseUrl) {
        SimpleClientHttpRequestFactory peticiones = new SimpleClientHttpRequestFactory();
        peticiones.setConnectTimeout(Duration.ofSeconds(5));
        peticiones.setReadTimeout(Duration.ofSeconds(25));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(peticiones).build();
    }
}
