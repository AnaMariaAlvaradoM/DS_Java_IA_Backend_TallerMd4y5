package com.devsenior.sillon.pago;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stripe.StripeClient;

/**
 * Configuración del bean de StripeClient.
 * Permite cambiar la dirección base de la API (STRIPE_URL_API) para usar simuladores.
 */
@Configuration
public class ConfiguracionStripe {

    @Bean
    public StripeClient stripeClient(StripeConfig stripeConfig) {
        return StripeClient.builder()
                .setApiKey(stripeConfig.claveSecreta())
                .setApiBase(stripeConfig.urlApi())
                .build();
    }
}
