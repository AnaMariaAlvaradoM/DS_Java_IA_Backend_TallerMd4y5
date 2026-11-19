package com.devsenior.sillon.pago.config;

import com.stripe.StripeClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionStripe {

    /** Cliente de Stripe con la clave secreta y la dirección base leída de STRIPE_URL_API. */
    @Bean
    public StripeClient stripeClient(StripeProperties propiedades) {
        return StripeClient.builder()
                .setApiKey(propiedades.claveSecreta())
                .setApiBase(propiedades.urlApi())
                .build();
    }
}
