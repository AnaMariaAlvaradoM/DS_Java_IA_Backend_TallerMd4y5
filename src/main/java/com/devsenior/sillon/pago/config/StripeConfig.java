package com.devsenior.sillon.pago.config;

import com.stripe.StripeClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    private final StripeProperties stripeProperties;

    public StripeConfig(StripeProperties stripeProperties) {
        this.stripeProperties = stripeProperties;
    }

    @Bean
    public StripeClient stripeClient() {
        String apiKey = stripeProperties.getSecretKey();

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Debe configurar la clave secreta de Stripe");
        }

        String apiBase = stripeProperties.getUrlApi();
        if (apiBase == null || apiBase.isBlank()) {
            apiBase = "https://api.stripe.com";
        }

        return StripeClient.builder()
                .setApiKey(apiKey)
                .setApiBase(apiBase)
                .build();
    }
}
