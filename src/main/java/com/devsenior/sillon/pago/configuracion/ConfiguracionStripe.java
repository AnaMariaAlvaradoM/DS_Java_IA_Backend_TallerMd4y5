
package com.devsenior.sillon.pago.configuracion;

import com.stripe.StripeClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PropiedadesStripe.class)
public class ConfiguracionStripe {

    @Bean
    public StripeClient stripeClient(PropiedadesStripe propiedades) {
        return StripeClient.builder()
                .setApiKey(propiedades.claveSecreta())
                .setApiBase(propiedades.urlApi())
                .build();
    }
}
