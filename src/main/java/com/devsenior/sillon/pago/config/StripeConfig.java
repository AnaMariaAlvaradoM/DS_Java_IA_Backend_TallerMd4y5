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

        // Si no viene clave o viene vacía, asigna una de prueba para permitir el arranque
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = "sk_test_51MockKeyForDev1234567890";
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
