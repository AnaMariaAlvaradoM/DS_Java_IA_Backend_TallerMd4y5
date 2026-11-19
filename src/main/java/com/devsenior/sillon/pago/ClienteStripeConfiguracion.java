package com.devsenior.sillon.pago;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stripe.StripeClient;

@Configuration
@EnableConfigurationProperties(ConfiguracionStripe.class)
public class ClienteStripeConfiguracion {

    @Bean
    public StripeClient stripeClient(ConfiguracionStripe configuracion) {
        String clave = configuracion.claveSecreta();
        if (clave == null || !(clave.startsWith("sk_test_") || clave.startsWith("rk_test_"))) {
            throw new IllegalStateException("Stripe requiere una clave de modo prueba");
        }
        return StripeClient.builder()
                .setApiKey(clave)
                .setApiBase(configuracion.urlApi())
                .build();
    }
}
