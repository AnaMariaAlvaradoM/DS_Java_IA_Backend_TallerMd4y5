package com.devsenior.sillon.pago;

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
        return StripeClient.builder()
                .setApiKey(stripeProperties.getClaveSecreta())
                .setApiBase(stripeProperties.getUrlApi())
                .build();
    }
}