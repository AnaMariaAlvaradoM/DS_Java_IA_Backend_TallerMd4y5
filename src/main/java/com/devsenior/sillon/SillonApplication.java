package com.devsenior.sillon;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

@SpringBootApplication(
        exclude =
                org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
                        .class)
public class SillonApplication {
    public static void main(String[] args) {
        SpringApplication.run(SillonApplication.class, args);
    }

    @Bean
    Clock reloj(@Value("${sillon.zona}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }

    @Bean
    com.stripe.StripeClient stripe(
            @Value("${sillon.stripe.api}") String api,
            @Value("${sillon.stripe.clave}") String clave) {
        return com.stripe.StripeClient.builder()
                .setApiBase(api)
                .setApiKey(clave.isBlank() ? "sin-configurar" : clave)
                .setConnectTimeout(4000)
                .setReadTimeout(15000)
                .setMaxNetworkRetries(0)
                .build();
    }

    @Bean
    RestClient http() {
        var settings =
                HttpClientSettings.defaults()
                        .withConnectTimeout(Duration.ofSeconds(4))
                        .withReadTimeout(Duration.ofSeconds(18));
        return RestClient.builder()
                .requestFactory(
                        org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
                                .simple()
                                .build(settings))
                .build();
    }
}
