package com.devsenior.sillon.pago;

import com.stripe.StripeClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StripeConfiguracion.class)
public class StripeClienteConfiguracion {
    @Bean
    public StripeClient stripeClient(StripeConfiguracion configuracion) {
        return StripeClient.builder()
                .setApiKey(configuracion.claveSecreta())
                .setApiBase(configuracion.urlApi())
                .build();
    }
}
