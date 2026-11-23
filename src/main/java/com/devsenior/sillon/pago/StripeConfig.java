package com.devsenior.sillon.pago;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stripe.StripeClient;

@Configuration
@EnableConfigurationProperties(StripePropiedades.class)
public class StripeConfig {

    @Bean
    public StripeClient stripeClient(StripePropiedades propiedades) {
        StripeClient.StripeClientBuilder builder = StripeClient.builder()
                .setApiKey(propiedades.claveSecreta());
        if (propiedades.urlApi() != null && !propiedades.urlApi().isBlank()) {
            builder.setApiBase(propiedades.urlApi());
        }
        return builder.build();
    }
}
