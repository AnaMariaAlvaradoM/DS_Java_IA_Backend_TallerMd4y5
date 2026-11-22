package com.devsenior.sillon.pago;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stripe.Stripe;
import com.stripe.StripeClient;

@Configuration
@EnableConfigurationProperties(StripePropiedades.class)
public class StripeConfig {

    @Bean
    public StripeClient stripeClient(StripePropiedades propiedades) {
        if (propiedades.urlApi() != null && !propiedades.urlApi().isBlank()) {
            Stripe.overrideApiBase(propiedades.urlApi());
        }
        return new StripeClient(propiedades.claveSecreta());
    }
}
