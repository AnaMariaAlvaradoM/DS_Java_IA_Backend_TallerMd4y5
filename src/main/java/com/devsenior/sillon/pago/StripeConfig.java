package com.devsenior.sillon.pago;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stripe.StripeClient;

@Configuration
public class StripeConfig {

    @Bean
    public StripeClient stripeClient(StripeProperties propiedades) {
        return StripeClient.builder()
                .setApiKey(propiedades.getClaveSecreta())
                .setApiBase(propiedades.getUrlApi())
                .build();
    }
}
