package com.devsenior.sillon.pago;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stripe.StripeClient;

/**
 * Cliente de Stripe con la dirección base configurable (STRIPE_URL_API).
 * El calificador la cambia por su simulador; en Render queda la de Stripe real.
 */
@Configuration
public class StripeConfig {

    @Bean
    public StripeClient stripeClient(@Value("${sillon.stripe.clave-secreta}") String claveSecreta,
            @Value("${sillon.stripe.url-api:https://api.stripe.com}") String urlApi) {
        return StripeClient.builder()
                .setApiKey(claveSecreta)
                .setApiBase(urlApi)
                .build();
    }
}
