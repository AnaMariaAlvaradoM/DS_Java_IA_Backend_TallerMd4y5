package com.devsenior.sillon.pago;

import com.stripe.Stripe;
import com.stripe.StripeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    @Value("${sillon.stripe.clave-secreta}")
    private String claveSecreta;

    @Value("${sillon.stripe.url-api:https://api.stripe.com}")
    private String urlApi;

    @Bean
    public StripeClient stripeClient() {
        Stripe.apiKey = claveSecreta;
        if (urlApi != null && !urlApi.isBlank()) {
            Stripe.overrideApiBase(urlApi);
        }
        return StripeClient.builder()
                .setApiKey(claveSecreta)
                .setApiBase(urlApi)
                .build();
    }
}
