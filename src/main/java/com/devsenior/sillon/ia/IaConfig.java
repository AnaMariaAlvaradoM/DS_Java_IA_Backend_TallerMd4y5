package com.devsenior.sillon.ia;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Fábrica HTTP con tiempo máximo: una IA lenta no puede dejar la petición colgada. */
@Configuration
@EnableConfigurationProperties(IaPropiedades.class)
public class IaConfig {

    @Bean
    public RestClient iaRestClient(RestClient.Builder builder, IaPropiedades propiedades) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(propiedades.timeout()));
        return builder.requestFactory(fabrica).build();
    }
}
