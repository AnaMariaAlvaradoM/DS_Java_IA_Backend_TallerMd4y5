package com.devsenior.sillon.ia;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(IaConfiguracion.class)
public class IaHttpConfiguracion {
    @Bean
    RestClient.Builder iaRestClientBuilder(IaConfiguracion configuracion) {
        HttpClient cliente = HttpClient.newBuilder().connectTimeout(configuracion.timeout()).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
        fabrica.setReadTimeout(configuracion.timeout());
        return RestClient.builder().requestFactory(fabrica);
    }
}
