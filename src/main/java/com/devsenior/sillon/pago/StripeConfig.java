package com.devsenior.sillon.pago;

import com.stripe.StripeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {
    @Bean
    public StripeClient stripeClient(
            @Value("${sillon.stripe.clave-secreta}") String clave,
            @Value("${sillon.stripe.url-api}") String urlApi) {
        return StripeClient.builder().setApiKey(clave).setApiBase(urlApi).build();
    }
}
